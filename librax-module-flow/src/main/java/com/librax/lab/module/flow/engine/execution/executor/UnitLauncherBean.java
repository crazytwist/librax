package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.enums.WaitingForEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.api.resource.AcquireRequest;
import com.librax.lab.module.flow.api.resource.AcquireResult;
import com.librax.lab.module.flow.api.resource.ResourcePool;
import com.librax.lab.module.flow.service.pipelineexecution.PipelineExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行单元启动器
 *
 * ★ 新增 PRE_ACQUIRE 模式，LAZY 模式原有逻辑完全不动
 *
 * resource_acquire_mode 取值：
 *   LAZY（默认）  子节点各自申请释放，不预占
 *   PRE_ACQUIRE   父节点预占所有子节点资源，子节点直接继承
 *
 * PRE_ACQUIRE 模式 YAML 配置示例：
 *   - id: s_unit_launcher
 *     type: UNIT_LAUNCHER
 *     params:
 *       unitPipelineKey: water_quality_sub
 *       resource_acquire_mode: PRE_ACQUIRE
 *       pre_acquire_resources:
 *         - resourceType: AGV
 *           zoneCode: ZONE-A
 *           holdTimeoutMs: 300000
 *         - resourceType: PH_METER
 *           zoneCode: ZONE-A
 *           holdTimeoutMs: 120000
 */
@Slf4j
@Component("unitLauncherBean")
@RequiredArgsConstructor
public class UnitLauncherBean implements StepExecutor {

    private final ApplicationContext applicationContext;
    // ★ 新增
    private final ResourcePool resourcePool;

    private PipelineExecutionService getExecutionService() {
        return applicationContext.getBean(PipelineExecutionService.class);
    }

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.UNIT_LAUNCHER;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        Map<String, Object> params = ctx.getInputParams();

        String  unitPipelineKey     = getString(params, "unitPipelineKey");
        Integer unitPipelineVersion = getInt(params, "unitPipelineVersion");
        int     maxRetry            = getInt(params, "maxRetry",     3);
        int     currentRetry        = getInt(params, "currentRetry", 0);

        if (unitPipelineKey == null) {
            return StepResult.fail("UNIT_CONFIG_MISSING", "unitPipelineKey 未配置");
        }

        if (currentRetry >= maxRetry) {
            log.warn("[UnitLauncher] 超过最大循环次数 executionId={} nodeId={} maxRetry={}",
                    ctx.getExecutionId(), ctx.getNodeId(), maxRetry);
            return StepResult.fail("UNIT_MAX_RETRY_EXCEEDED",
                    String.format("执行单元循环次数已达上限 %d 次", maxRetry));
        }

        // ★ 新增：PRE_ACQUIRE 模式先预占资源，失败则等待重试
        String acquireMode = getString(params, "resource_acquire_mode");
        if ("PRE_ACQUIRE".equals(acquireMode)) {
            StepResult preAcquireResult = doPreAcquire(ctx, params);
            if (preAcquireResult != null) {
                // 有一个资源没拿到，已回滚，等待重调度
                return preAcquireResult;
            }
        }

        // ── 原有启动子流程逻辑，完全不动 ─────────────────────────────
        Map<String, Object> childParams = new HashMap<>(params);
        childParams.put("parentCallbackToken", ctx.getCallbackToken());
        childParams.put("currentRetry", currentRetry);
        childParams.put("maxRetry",     maxRetry);

        PipelineExecutionService executionService = getExecutionService();
        String childExecutionId = executionService.startChild(
                unitPipelineKey,
                unitPipelineVersion,
                ctx.getExecutionId(),
                ctx.getCallbackToken(),
                childParams);

        log.info("[UnitLauncher] 子流程已启动 parentExecutionId={} childExecutionId={} " +
                        "retry={}/{} mode={}",
                ctx.getExecutionId(), childExecutionId,
                currentRetry + 1, maxRetry,
                acquireMode != null ? acquireMode : "LAZY");

        return StepResult.waiting(WaitingForEnum.CHILD_EXECUTION, Map.of(
                "childExecutionId", childExecutionId,
                "unitPipelineKey",  unitPipelineKey,
                "currentRetry",     currentRetry,
                "maxRetry",         maxRetry
        ));
    }

    // ================================================================
    // ★ 新增：PRE_ACQUIRE 预占逻辑
    // ================================================================

    /**
     * 预占子流程所有需要的资源
     *
     * 成功：所有资源全部拿到，返回 null（继续执行）
     * 失败：任意一个拿不到，回滚已申请的，返回 WAITING 等重试
     *
     * 预占的资源：
     *   holderKey = parentExecutionId:nodeId:attempt（父节点的 holderKey）
     *   子节点申请时，ResourcePool.tryInheritFromParent 查到这条 ACQUIRED 记录直接继承
     *   子流程全部完成后，DAG 引擎对父节点执行 releaseByHolder 统一释放
     */
    @SuppressWarnings("unchecked")
    private StepResult doPreAcquire(StepDispatchContext ctx, Map<String, Object> params) {
        Object rawList = params.get("pre_acquire_resources");
        if (!(rawList instanceof List)) return null;

        List<Map<String, Object>> resourceList = (List<Map<String, Object>>) rawList;
        if (CollectionUtils.isEmpty(resourceList)) return null;

        // 记录已申请到的，失败时用来回滚
        List<String> acquiredIds = new ArrayList<>();

        // 父节点的 holderKey，子节点继承时 Redis 锁的持有者不变
        String holderKey = ctx.getExecutionId() + ":" + ctx.getNodeId() + ":" + ctx.getAttempt();

        for (Map<String, Object> res : resourceList) {
            String resourceType  = getString(res, "resourceType");
            String zoneCode      = getString(res, "zoneCode");
            long   holdTimeoutMs = getLong(res, "holdTimeoutMs", 300_000L);

            if (resourceType == null) {
                log.warn("[UnitLauncher] pre_acquire_resources 缺少 resourceType，跳过");
                continue;
            }

            AcquireRequest req = AcquireRequest.builder()
                    .executionId(ctx.getExecutionId())
                    .nodeId(ctx.getNodeId())
                    .attempt(ctx.getAttempt())
                    .resourceType(resourceType)
                    .zoneCode(zoneCode)
                    .holderKey(holderKey)
                    .holdTimeoutMs(holdTimeoutMs)
                    .build();

            AcquireResult result = resourcePool.acquire(req);

            if (!result.isSuccess()) {
                // 申请失败：回滚所有已申请的资源
                log.warn("[UnitLauncher] 预占失败，回滚已申请资源 " +
                                "resourceType={} zone={} reason={} executionId={}",
                        resourceType, zoneCode, result.getFailReason(), ctx.getExecutionId());

                for (String acquiredId : acquiredIds) {
                    try {
                        resourcePool.release(acquiredId, holderKey);
                    } catch (Exception e) {
                        log.error("[UnitLauncher] 回滚释放失败 resourceId={}", acquiredId, e);
                    }
                }

                // 返回 WAITING，等 suggestRetryMs 后由 DAG 引擎重新调度
                return StepResult.waiting(WaitingForEnum.RESOURCE, Map.of(
                        "preAcquireFailed", true,
                        "failedResource",   resourceType,
                        "failReason",       result.getFailReason().name(),
                        "retryAfterMs",     result.getSuggestRetryMs()
                ));
            }

            // 申请成功：记录 hold，供 tryInheritFromParent 查询
            acquiredIds.add(result.getResourceId());
            resourcePool.recordHold(
                    ctx.getExecutionId(),
                    ctx.getNodeId(),
                    ctx.getAttempt(),
                    result.getResourceId(),
                    resourceType);

            log.info("[UnitLauncher] 预占成功 resourceId={} type={} zone={} executionId={}",
                    result.getResourceId(), resourceType, zoneCode, ctx.getExecutionId());
        }

        log.info("[UnitLauncher] 所有资源预占完成 count={} executionId={}",
                acquiredIds.size(), ctx.getExecutionId());
        return null; // 全部成功，继续启动子流程
    }

    // ================================================================
    // 原有工具方法（不动）
    // ================================================================

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private int getInt(Map<String, Object> params, String key, int defaultVal) {
        Object v = params.get(key);
        if (v == null) return defaultVal;
        return ((Number) v).intValue();
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? ((Number) v).intValue() : null;
    }

    // ★ 新增
    private long getLong(Map<String, Object> params, String key, long defaultVal) {
        Object v = params.get(key);
        if (v == null) return defaultVal;
        return ((Number) v).longValue();
    }
}