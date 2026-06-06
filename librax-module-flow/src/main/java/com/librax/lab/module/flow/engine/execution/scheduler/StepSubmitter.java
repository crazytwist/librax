package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.api.dispatch.DispatchCallback;
import com.librax.lab.module.flow.api.dispatch.DispatchSpi;
import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.material.MaterialCheckRequest;
import com.librax.lab.module.flow.api.material.MaterialCheckResult;
import com.librax.lab.module.flow.api.material.MaterialCheckSpi;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.api.resource.AcquireRequest;
import com.librax.lab.module.flow.api.resource.AcquireResult;
import com.librax.lab.module.flow.api.resource.ResourcePool;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.context.ExecutionContextManager;
import com.librax.lab.module.flow.engine.execution.executor.StepExecutorFactory;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import com.librax.lab.module.infra.mdc.ExecutionMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.librax.lab.module.flow.api.scheduler.SchedulerConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class StepSubmitter {

    private final StepExecutionMapper stepMapper;
    private final StepStateMachine stepStateMachine;
    private final ExecutionContextManager contextManager;
    private final StepExecutorFactory executorFactory;
    private final BranchSkipper branchSkipper;
    private final Executor stepExecutorPool;
    private final DispatchSpiFactory dispatchSpiFactory;
    private final ResourcePool resourcePool;
    private final MaterialCheckSpi materialCheckSpi;
    private final ResourceWaitRegistry resourceWaitRegistry;

    /**
     * 资源等待默认超时时间：5分钟。
     * 步骤在 PENDING 状态等待资源超过此值后，触发 RESOURCE_WAIT_TIMEOUT 失败。
     */
    private static final long DEFAULT_RESOURCE_WAIT_TIMEOUT_MS = 300_000L;

    // ================================================================
    // 主入口
    // ================================================================

    public void submit(String executionId,
                       PipelineGraph graph,
                       StepNode node,
                       DagSchedulerCallback dagCallback) {

        // 1. 查步骤记录（只查一次）
        StepExecutionDO stepDO = stepMapper.selectLatestAttempt(executionId, node.getNodeId());
        if (stepDO == null) {
            log.warn("[StepSubmitter] 步骤记录不存在 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }

        // CONDITION 同步执行，不需要资源，也不需要 input_snapshot
        if (node.getStepType() == StepTypeEnum.CONDITION) {
            executeCondition(executionId, graph, node, stepDO, dagCallback);
            return;
        }

        // 2. 提前解析入参（后续 tryStart 写 snapshot + buildContext 两处复用，不重复解析）
        Map<String, Object> inputParams = resolveInputParams(executionId, node);

        // 2.5 物料核验（物料不足直接 fail，不进资源调度）
        if (node.getPipelineStepId() != null) {
            MaterialCheckResult checkResult = materialCheckSpi.check(
                    MaterialCheckRequest.builder()
                            .pipelineStepId(node.getPipelineStepId())
                            .executionId(executionId)
                            .nodeId(node.getNodeId())
                            .zoneCode(node.getZoneCode())
                            .build());
            if (!checkResult.isPassed()) {
                String reasons = String.join("; ", checkResult.getFailReasons());
                log.warn("[StepSubmitter] 物料核验不通过 executionId={} nodeId={} reasons={}",
                        executionId, node.getNodeId(), reasons);
                dagCallback.onStepComplete(executionId, graph, node.getNodeId(),
                        stepDO.getAttempt(),
                        StepResult.fail("MATERIAL_CHECK_FAILED", "物料不足: " + reasons));
                return;
            }
        }

        // 3. 申请资源（用 resource_enabled 开关控制）
        String resourceId = null;
        String holderKey = buildHolderKey(executionId, node.getNodeId(), stepDO.getAttempt());

        if (node.isResourceEnabled()) {
            AcquireResult result = resourcePool.acquire(AcquireRequest.builder()
                    .resourceType(node.getDeviceType())
                    .zoneCode(node.getZoneCode())
                    .holderKey(holderKey)
                    .holdTimeoutMs(resolveTimeout(node))
                    .allowSharedFallback(true)
                    .build());

            if (!result.isSuccess()) {
                // 检查资源等待是否超时：queuedAt + resourceWaitTimeoutMs < now
                if (isResourceWaitTimeout(stepDO, node)) {
                    long waitTimeoutMs = resolveResourceWaitTimeout(node);
                    long elapsedMs = Duration.between(stepDO.getQueuedAt(), LocalDateTime.now()).toMillis();
                    log.warn("[StepSubmitter] 资源等待超时 executionId={} nodeId={} elapsed={}ms timeout={}ms",
                            executionId, node.getNodeId(), elapsedMs, waitTimeoutMs);
                    dagCallback.onStepComplete(executionId, graph, node.getNodeId(), stepDO.getAttempt(),
                            StepResult.fail("RESOURCE_WAIT_TIMEOUT",
                                    String.format("资源等待超时: 已等待%dms, 超时阈值%dms, 资源类型=%s",
                                            elapsedMs, waitTimeoutMs, node.getDeviceType())));
                    return;
                }
                // 注册到等待表，资源释放时由 ResourceReleaseWakeupListener 触发唤醒
                resourceWaitRegistry.register(executionId, node.getDeviceType(), node.getZoneCode());
                log.info("[StepSubmitter] 资源不可用，已注册等待 executionId={} nodeId={} type={} reason={}",
                        executionId, node.getNodeId(), node.getDeviceType(), result.getReason());
                return;
            }

            resourceId = result.getResourceId();
            writeResourceHold(executionId, node.getNodeId(), stepDO.getAttempt(), resourceId, node.getDeviceType());
            log.info("[StepSubmitter] 资源已申请 executionId={} nodeId={} resourceId={}", executionId, node.getNodeId(), resourceId);
            //  把 resourceId 注入入参，执行器用它发设备指令
            inputParams.put(CONTEXT_KEY_RESOURCE_ID, resourceId);
        }

        // 4. CAS 抢占步骤（PENDING → RUNNING），同时写 input_snapshot
        boolean started = stepStateMachine.tryStart(
                executionId, node.getNodeId(), stepDO.getAttempt(), inputParams);
        if (!started) {
            log.warn("[StepSubmitter] 步骤已被抢占 executionId={} nodeId={}", executionId, node.getNodeId());
            if (resourceId != null) {
                rollbackResource(executionId, node.getNodeId(), stepDO.getAttempt(), resourceId, holderKey);
            }
            return;
        }

        // 5. tryStart 成功后重新查，拿到生成的 callbackToken
        stepDO = stepMapper.selectLatestAttempt(executionId, node.getNodeId());
        final StepExecutionDO finalStepDO = stepDO;


        // 7. 组装上下文并分发（复用已解析的 inputParams，不重复解析）
        StepDispatchContext ctx = buildContext(
                executionId, graph, node, finalStepDO, dagCallback, inputParams);
        DispatchSpi spi = dispatchSpiFactory.getSpi(node.getDispatchMode());
        final String finalResourceId = resourceId;

        stepExecutorPool.execute(ExecutionMdc.wrap(() -> {
            try {
                spi.dispatch(ctx);
            } catch (Exception e) {
                log.error("[StepSubmitter] 分发异常 executionId={} nodeId={}",
                        executionId, node.getNodeId(), e);
                if (finalResourceId != null) {
                    releaseResource(executionId, node.getNodeId(),
                            finalStepDO.getAttempt(), finalResourceId,
                            holderKey, "DISPATCH_ERROR");
                }
                dagCallback.onStepComplete(executionId, graph,
                        node.getNodeId(), finalStepDO.getAttempt(),
                        StepResult.fail("DISPATCH_ERROR", e.getMessage()));
            }
        }, executionId, node.getNodeId(), finalStepDO.getAttempt()));
    }

    // ================================================================
    // CONDITION 节点
    // ================================================================

    private void executeCondition(String executionId,
                                  PipelineGraph graph,
                                  StepNode node,
                                  StepExecutionDO stepDO,
                                  DagSchedulerCallback dagCallback) {
        int attempt = stepDO.getAttempt();

        // CONDITION 不需要 input_snapshot，传 null
        boolean acquired = stepStateMachine.tryStart(executionId, node.getNodeId(), attempt);
        if (!acquired) {
            log.warn("[StepSubmitter] CONDITION 步骤已被抢占 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }

        try {
            Map<String, Object> inputParams = resolveInputParams(executionId, node);
            StepDispatchContext ctx = buildContext(
                    executionId, graph, node, stepDO, dagCallback, inputParams);
            StepExecutor conditionExecutor = executorFactory.getExecutor(StepTypeEnum.CONDITION);
            StepResult result = conditionExecutor.execute(ctx);

            if (result.isSuccess()) {
                markUnmatchedBranchesSkipped(executionId, graph, node, result);
            }

            dagCallback.onStepComplete(executionId, graph, node.getNodeId(), attempt, result);

        } catch (Exception e) {
            log.error("[StepSubmitter] CONDITION 节点异常 executionId={} nodeId={}",
                    executionId, node.getNodeId(), e);
            dagCallback.onStepComplete(executionId, graph, node.getNodeId(), attempt,
                    StepResult.fail("CONDITION_EVAL_FAIL", e.getMessage()));
        }
    }

    // ================================================================
    // 分支跳过
    // ================================================================

    private void markUnmatchedBranchesSkipped(String executionId,
                                              PipelineGraph graph,
                                              StepNode conditionNode,
                                              StepResult result) {
        String branchName = (String) result.getOutputs().get("branchName");
        log.info("[StepSubmitter] 条件节点分支选择 executionId={} nodeId={} branchName={}",
                executionId, conditionNode.getNodeId(), branchName);

        Map<String, String> allBranches = conditionNode.getAllBranches();
        if (allBranches == null) return;

        for (Map.Entry<String, String> entry : allBranches.entrySet()) {
            if (!entry.getKey().equals(branchName)) {
                branchSkipper.markBranchSkipped(executionId, graph,
                        entry.getValue(), conditionNode.getNodeId());
            }
        }
    }

    // ================================================================
    // 上下文组装（接收已解析的 inputParams，不在内部重复解析）
    // ================================================================

    private StepDispatchContext buildContext(String executionId,
                                             PipelineGraph graph,
                                             StepNode node,
                                             StepExecutionDO stepDO,
                                             DagSchedulerCallback dagCallback,
                                             Map<String, Object> inputParams) {
        DispatchCallback callback = (execId, nodeId, attempt, success, outputs, errCode, errMsg) ->
                dagCallback.onStepComplete(execId, graph, nodeId, attempt,
                        success ? StepResult.ok(outputs) : StepResult.fail(errCode, errMsg));

        return StepDispatchContext.builder()
                .executionId(executionId)
                .nodeId(node.getNodeId())
                .attempt(stepDO.getAttempt())
                .callbackToken(stepDO.getCallbackToken())
                .stepType(node.getStepType().name())
                .taskType(node.getTaskType())
                .stepKey(node.getStepKey())
                .stepName(node.getName())
                .inputParams(inputParams)
                .zoneCode(node.getZoneCode())
                .priority(0)
                .timeoutMs(node.getTimeoutMs() != null ? node.getTimeoutMs() : 30_000L)
                .maxAttempts(node.getMaxAttempts() != null ? node.getMaxAttempts() : 3)
                .deviceType(node.getDeviceType())
                .commandCode(node.getCommand())
                .executor(node.getExecutor())
                .beanName(node.getBeanName())
                .methodName(node.getMethodName())
                .chainId(node.getChainId())
                .conditionExpr(node.getConditionExpr())
                .allBranches(node.getAllBranches())
                .mockOutput(node.getMockOutput())
                .callback(callback)
                .build();
    }

    private Map<String, Object> resolveInputParams(String executionId, StepNode node) {
        Map<String, Object> inputParams = contextManager
                .getNodeOutput(executionId, CONTEXT_KEY_INPUT);
        Map<String, Object> mappedParams = contextManager.resolveInputMapping(
                executionId, node.getInputMapping(), inputParams);
        Map<String, Object> merged = new HashMap<>(node.getParams());
        if (mappedParams != null) merged.putAll(mappedParams);
        // 递归解析 merged 中任意层级的 ${...} 表达式（兼容 process_json 等嵌套结构）
        @SuppressWarnings("unchecked")
        Map<String, Object> resolved = (Map<String, Object>) contextManager.resolveDeep(executionId, merged, inputParams);
        return resolved;
    }

    // ================================================================
    // 资源持有写入 / 释放
    // ================================================================

    private void writeResourceHold(String executionId, String nodeId,
                                   int attempt, String resourceId, String resourceType) {
        // 通过 ResourcePool SPI 写 hold 记录，flow 不直接依赖 resource 模块的 Mapper
        resourcePool.recordHold(executionId, nodeId, attempt, resourceId, resourceType);
        // markResourceAcquired 操作 pe_step_execution（flow 自己的表），通过 stateMachine 调
        stepStateMachine.markResourceAcquired(executionId, nodeId, attempt);
    }

    /**
     * 正常释放：步骤完成 / 超时 / DEAD 时调
     */
    public void releaseResource(String executionId, String nodeId,
                                int attempt, String resourceId,
                                String holderKey, String reason) {
        resourcePool.release(resourceId, holderKey);
        resourcePool.markHoldReleased(executionId, nodeId, attempt, reason);
        stepStateMachine.clearResourceAcquired(executionId, nodeId, attempt);
        log.info("[StepSubmitter] 资源已释放 executionId={} nodeId={} resourceId={} reason={}",
                executionId, nodeId, resourceId, reason);
    }

    /**
     * 回滚释放：tryStart 失败时调，保留审计记录
     */
    private void rollbackResource(String executionId, String nodeId,
                                  int attempt, String resourceId, String holderKey) {
        resourcePool.release(resourceId, holderKey);
        resourcePool.markHoldReleased(executionId, nodeId, attempt, "ROLLBACK");
        stepStateMachine.clearResourceAcquired(executionId, nodeId, attempt);
        log.info("[StepSubmitter] 资源回滚 executionId={} nodeId={} resourceId={}",
                executionId, nodeId, resourceId);
    }

    /**
     * 按持有记录释放（给 StepSuccessHandler / StepFailureHandler / TimeoutWatchdog 用）
     */
    public void releaseIfHeld(String executionId, String nodeId,
                              int attempt, String reason) {
        String resourceId = resourcePool.queryHeldResourceId(executionId, nodeId, attempt);
        if (resourceId == null) return;

        String holderKey = buildHolderKey(executionId, nodeId, attempt);
        releaseResource(executionId, nodeId, attempt, resourceId, holderKey, reason);
    }

    // ================================================================
    // 工具
    // ================================================================

    public static String buildHolderKey(String executionId, String nodeId, int attempt) {
        return executionId + ":" + nodeId + ":" + attempt;
    }

    private long resolveTimeout(StepNode node) {
        return node.getTimeoutMs() != null ? node.getTimeoutMs() : 30_000L;
    }

    /**
     * 判断步骤的资源等待是否超时。
     * 用 queuedAt（步骤进入 PENDING 的时间）作为等待起点，
     * 超过 resourceWaitTimeoutMs 后视为资源等待超时。
     */
    private boolean isResourceWaitTimeout(StepExecutionDO stepDO, StepNode node) {
        if (stepDO.getQueuedAt() == null) {
            return false;
        }
        long waitTimeoutMs = resolveResourceWaitTimeout(node);
        long elapsedMs = Duration.between(stepDO.getQueuedAt(), LocalDateTime.now()).toMillis();
        return elapsedMs > waitTimeoutMs;
    }

    private long resolveResourceWaitTimeout(StepNode node) {
        return node.getResourceWaitTimeoutMs() != null
                ? node.getResourceWaitTimeoutMs()
                : DEFAULT_RESOURCE_WAIT_TIMEOUT_MS;
    }

    public interface DagSchedulerCallback {
        void onStepComplete(String executionId, PipelineGraph graph,
                            String nodeId, int attempt, StepResult result);

        Map<String, Object> resolveInputParams(String executionId, StepNode node);
    }
}