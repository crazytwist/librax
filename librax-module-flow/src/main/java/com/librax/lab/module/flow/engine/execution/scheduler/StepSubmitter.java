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
import com.librax.lab.module.flow.api.sample.SampleContextSpi;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    /** 样本上下文 SPI，可选——lab 模块未部署时为空列表，不影响流程运行 */
    @Autowired(required = false)
    private List<SampleContextSpi> sampleContextSpis;

    /** 回调基础地址，用于构造 callbackUrl 透传给外部服务 */
    @Value("${librax.flow.callback-base-url:http://localhost:48080/app-api}")
    private String callbackBaseUrl;

    /** 通用步骤回调路径（非设备类，如 HTTP Task / MANUAL） */
    private static final String STEP_CALLBACK_PATH   = "/flow/callback/step-complete";
    /** 设备步骤回调路径模版（含 executionId / nodeId 占位符） */
    private static final String DEVICE_CALLBACK_PATH = "/device/callback/%s/%s";

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

        // 2. 提前生成 callbackToken，确保 input_snapshot 与实际发出的请求 body 中 token 完全一致
        String callbackToken = UUID.randomUUID().toString().replace("-", "");

        // 3. 解析入参（使用预生成的 token，snapshot 与 body 保持一致）
        Map<String, Object> inputParams = resolveInputParams(executionId, node, stepDO, callbackToken);

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

        // 4. CAS 抢占步骤（PENDING → RUNNING），同时写 input_snapshot（含真实 callbackToken）
        boolean started = stepStateMachine.tryStart(
                executionId, node.getNodeId(), stepDO.getAttempt(), inputParams, callbackToken);
        if (!started) {
            log.warn("[StepSubmitter] 步骤已被抢占 executionId={} nodeId={}", executionId, node.getNodeId());
            if (resourceId != null) {
                rollbackResource(executionId, node.getNodeId(), stepDO.getAttempt(), resourceId, holderKey);
            }
            return;
        }

        // 5. tryStart 成功后重新查，拿到完整的 stepDO（含 stepType 等字段供 buildContext 使用）
        stepDO = stepMapper.selectLatestAttempt(executionId, node.getNodeId());
        final StepExecutionDO finalStepDO = stepDO;

        // 6. 组装上下文并分发（复用已解析的 inputParams，不重复解析）
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
            Map<String, Object> inputParams = resolveInputParams(executionId, node, stepDO);
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

    /**
     * CONDITION 节点专用重载：token 对 CONDITION 无意义，传空串即可。
     */
    private Map<String, Object> resolveInputParams(String executionId, StepNode node,
                                                    StepExecutionDO stepDO) {
        return resolveInputParams(executionId, node, stepDO, "");
    }

    /**
     * 解析步骤入参。
     *
     * @param callbackToken 由 submit() 在 tryStart 之前预生成，确保 snapshot 与请求 body 一致
     */
    private Map<String, Object> resolveInputParams(String executionId, StepNode node,
                                                     StepExecutionDO stepDO, String callbackToken) {
        // 1. 读取流程启动时的初始参数（含 sampleId 等业务 key）
        Map<String, Object> launchParams = contextManager
                .getNodeOutput(executionId, CONTEXT_KEY_INPUT);

        // 设备步骤（INSTRUMENT）回调到 device/callback，由 DeviceCallbackHandler 负责释放设备后再推进 DAG
        // 其他步骤（HTTP Task / MANUAL 等）直接回调到 flow/callback/step-complete
        String callbackUrl = (node.getStepType() == StepTypeEnum.INSTRUMENT)
                ? callbackBaseUrl + String.format(DEVICE_CALLBACK_PATH, executionId, node.getNodeId())
                : callbackBaseUrl + STEP_CALLBACK_PATH;

        // 系统变量：可在参数模版中通过 ${sys.executionId} / ${sys.nodeId} /
        //           ${sys.callbackToken} / ${sys.callbackUrl} 引用
        Map<String, Object> sysVars = new HashMap<>();
        sysVars.put("executionId",   executionId);
        sysVars.put("nodeId",        node.getNodeId());
        sysVars.put("callbackToken", callbackToken);
        sysVars.put("callbackUrl",   callbackUrl);

        // 2. 解析步骤级 inputMapping + YAML 静态 params
        Map<String, Object> mappedParams = contextManager.resolveInputMapping(
                executionId, node.getInputMapping(), launchParams, sysVars);
        Map<String, Object> merged = new HashMap<>(node.getParams());
        if (mappedParams != null) merged.putAll(mappedParams);

        // 3. 递归解析 ${...} 表达式
        @SuppressWarnings("unchecked")
        Map<String, Object> resolved = (Map<String, Object>) contextManager.resolveDeep(executionId, merged, launchParams, sysVars);

        // 4. 将样本 experimentParams 作为基础层注入（低优先级，YAML 显式配置的参数覆盖此层）
        //    前提：launchParams 中包含 sampleId（由调用方启动流程时传入）
        if (sampleContextSpis != null && !sampleContextSpis.isEmpty() && launchParams != null) {
            Object sampleIdVal = launchParams.get("sampleId");
            if (sampleIdVal != null) {
                String sampleId = sampleIdVal.toString();
                try {
                    Map<String, Object> sampleParams = sampleContextSpis.get(0).getExperimentParams(sampleId);
                    if (sampleParams != null && !sampleParams.isEmpty()) {
                        // sample params 打底，resolved 覆盖（步骤配置优先级更高）
                        Map<String, Object> withBase = new HashMap<>(sampleParams);
                        withBase.putAll(resolved);
                        resolved = withBase;
                        log.debug("[StepSubmitter] 样本实验参数已注入 sampleId={} keys={}",
                                sampleId, sampleParams.keySet());
                    }
                } catch (Exception e) {
                    log.warn("[StepSubmitter] 样本实验参数注入失败，跳过 sampleId={}", sampleId, e);
                }
            }
        }

        // 5. 将流程启动参数（launchParams）作为最低优先级底层合并
        //    使 requestTemplate 中可直接用 ${taskId} 引用，无需在 YAML 里显式 inputMapping
        //    优先级：sampleParams < launchParams < YAML params/inputMapping < 系统字段
        if (launchParams != null && !launchParams.isEmpty()) {
            Map<String, Object> withLaunch = new HashMap<>(launchParams);
            withLaunch.putAll(resolved);
            resolved = withLaunch;
        }

        // 6. 自动内置四个系统字段，外部服务无需在 YAML 里配置即可直接使用
        //    优先级最高，始终覆盖（保证外部拿到的一定是当前步骤真实值）
        resolved.put("executionId",   executionId);
        resolved.put("nodeId",        node.getNodeId());
        resolved.put("callbackToken", callbackToken);
        resolved.put("callbackUrl",   callbackUrl);

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