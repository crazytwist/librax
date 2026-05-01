package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.api.dispatch.DispatchCallback;
import com.librax.lab.module.flow.api.dispatch.DispatchSpi;
import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public void submit(String executionId,
                       PipelineGraph graph,
                       StepNode node,
                       DagSchedulerCallback dagCallback) {

        // 1. 查步骤记录（只查一次，全程用这一个）
        StepExecutionDO stepDO = stepMapper.selectLatestAttempt(
                executionId, node.getNodeId());
        if (stepDO == null) {
            log.warn("[StepSubmitter] 步骤记录不存在 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }

        // CONDITION 同步执行，不需要资源
        if (node.getStepType() == StepTypeEnum.CONDITION) {
            executeCondition(executionId, graph, node, stepDO, dagCallback);
            return;
        }

        // 2. 申请资源（用 resource_enabled 开关控制）
        String resourceId = null;
        String holderKey  = buildHolderKey(
                executionId, node.getNodeId(), stepDO.getAttempt());

        if (node.isResourceEnabled()) {
            AcquireResult result = resourcePool.acquire(AcquireRequest.builder()
                    .resourceType(node.getDeviceType())
                    .zoneCode(node.getZoneCode())          // ★ 从 node 取，不从 graph 取
                    .holderKey(holderKey)
                    .holdTimeoutMs(resolveTimeout(node))
                    .allowSharedFallback(true)
                    .build());

            if (!result.isSuccess()) {
                // 资源不足：直接返回，等下轮 DagScheduler 重新调度
                // 不改步骤状态，不消耗重试次数
                log.info("[StepSubmitter] 资源不可用，等待下轮调度 " +
                                "executionId={} nodeId={} reason={}",
                        executionId, node.getNodeId(), result.getReason());
                return;
            }

            resourceId = result.getResourceId();

            // 写 pe_step_resource_hold（Redis 锁 + DB 记录双写）
            writeResourceHold(executionId, node.getNodeId(),
                    stepDO.getAttempt(), resourceId, node.getDeviceType());

            log.info("[StepSubmitter] 资源已申请 executionId={} nodeId={} resourceId={}",
                    executionId, node.getNodeId(), resourceId);
        }

        // 3. CAS 抢占步骤（PENDING → RUNNING）
        boolean started = stepStateMachine.tryStart(
                executionId, node.getNodeId(), stepDO.getAttempt());
        if (!started) {
            log.warn("[StepSubmitter] 步骤已被抢占 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            // tryStart 失败，回滚资源
            if (resourceId != null) {
                rollbackResource(executionId, node.getNodeId(),
                        stepDO.getAttempt(), resourceId, holderKey);
            }
            return;
        }

        // tryStart 内部生成 token 并写入 pe_step_execution.callback_token
        stepDO = stepMapper.selectLatestAttempt(executionId, node.getNodeId());
        final StepExecutionDO finalStepDO = stepDO;  // ← 加这一行

        // 4. 组装上下文并分发
        StepDispatchContext ctx = buildContext(
                executionId, graph, node, finalStepDO, dagCallback, resourceId);
        DispatchSpi spi = dispatchSpiFactory.getSpi(node.getDispatchMode());
        final String finalResourceId = resourceId;

        stepExecutorPool.execute(() -> {
            try {
                spi.dispatch(ctx);
            } catch (Exception e) {
                log.error("[StepSubmitter] 分发异常 executionId={} nodeId={}", executionId, node.getNodeId(), e);
                if (finalResourceId != null) {
                    releaseResource(executionId, node.getNodeId(), finalStepDO.getAttempt(), finalResourceId,
                            holderKey, "DISPATCH_ERROR");
                }
                dagCallback.onStepComplete(executionId, graph,
                        node.getNodeId(), finalStepDO.getAttempt(),
                        StepResult.fail("DISPATCH_ERROR", e.getMessage()));
            }
        });
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

        boolean acquired = stepStateMachine.tryStart(executionId, node.getNodeId(), attempt);
        if (!acquired) {
            log.warn("[StepSubmitter] CONDITION 步骤已被抢占 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }

        try {
            StepDispatchContext ctx = buildContext(
                    executionId, graph, node, stepDO, dagCallback, null);
            StepExecutor conditionExecutor = executorFactory.getExecutor(StepTypeEnum.CONDITION);
            StepResult result = conditionExecutor.execute(ctx);

            if (result.isSuccess()) {
                markUnmatchedBranchesSkipped(executionId, graph, node, result);
            }

            dagCallback.onStepComplete(executionId, graph,
                    node.getNodeId(), attempt, result);

        } catch (Exception e) {
            log.error("[StepSubmitter] CONDITION 节点异常 executionId={} nodeId={}",
                    executionId, node.getNodeId(), e);
            dagCallback.onStepComplete(executionId, graph,
                    node.getNodeId(), attempt,
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
    // 上下文组装
    // ================================================================

    private StepDispatchContext buildContext(String executionId,
                                             PipelineGraph graph,
                                             StepNode node,
                                             StepExecutionDO stepDO,
                                             DagSchedulerCallback dagCallback,
                                             String resourceId) {
        Map<String, Object> inputParams = resolveInputParams(executionId, node);

        // ★ 把 resourceId 塞进入参,执行器用它发设备指令
        if (resourceId != null) {
            inputParams.put(CONTEXT_KEY_RESOURCE_ID, resourceId);
        }

        DispatchCallback callback = (execId, nodeId, attempt, success, outputs, errCode, errMsg) ->
                dagCallback.onStepComplete(execId, graph, nodeId, attempt,
                        success
                                ? StepResult.ok(outputs)
                                : StepResult.fail(errCode, errMsg));

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
        return merged;
    }

    // ================================================================
    // 辅助
    // ================================================================

    /**
     * 持有者标识:executionId:nodeId:attempt
     */
    public static String buildHolderKey(String executionId, String nodeId, int attempt) {
        return executionId + ":" + nodeId + ":" + attempt;
    }

    public interface DagSchedulerCallback {
        void onStepComplete(String executionId, PipelineGraph graph,
                            String nodeId, int attempt, StepResult result);
        Map<String, Object> resolveInputParams(String executionId, StepNode node);
    }


    private void writeResourceHold(String executionId, String nodeId,
                                   int attempt, String resourceId, String resourceType) {
        // 通过 SPI 调，flow 不接触 resource 模块的 Mapper
        resourcePool.recordHold(executionId, nodeId, attempt, resourceId, resourceType);
        // markResourceAcquired 操作的是 pe_step_execution，属于 flow 模块自己的表，保留
        stepMapper.markResourceAcquired(executionId, nodeId, attempt);
    }


    private long resolveTimeout(StepNode node) {
        return node.getTimeoutMs() != null ? node.getTimeoutMs() : 30_000L;
    }

    /**
     * 正常释放：步骤完成/超时/DEAD 时调
     * 同时释放 Redis 锁 + 更新 DB hold 记录
     */
    public void releaseResource(String executionId, String nodeId,
                                int attempt, String resourceId,
                                String holderKey, String reason) {
        resourcePool.release(resourceId, holderKey);
        resourcePool.markHoldReleased(executionId, nodeId, attempt, reason);
        stepMapper.clearResourceAcquired(executionId, nodeId, attempt);
        log.info("[StepSubmitter] 资源已释放 executionId={} nodeId={} resourceId={} reason={}",
                executionId, nodeId, resourceId, reason);
    }

    /**
     * 回滚释放：tryStart 失败时调，此时 hold 记录已写入需要清除
     */
    private void rollbackResource(String executionId, String nodeId,
                                  int attempt, String resourceId, String holderKey) {
        resourcePool.release(resourceId, holderKey);
        // 回滚：物理删除刚写入的 hold 记录（通过 SPI 调）
        // 这里需要在 ResourcePool 接口再加一个方法，或者直接让 markHoldReleased 处理
        // 推荐直接用 markHoldReleased + reason = "ROLLBACK"，保留审计记录
        resourcePool.markHoldReleased(executionId, nodeId, attempt, "ROLLBACK");
        stepMapper.clearResourceAcquired(executionId, nodeId, attempt);
        log.info("[StepSubmitter] 资源回滚 executionId={} nodeId={} resourceId={}",
                executionId, nodeId, resourceId);
    }

    // ── 查持有记录（给 releaseIfHeld 用）────────────────────────
    public void releaseIfHeld(String executionId, String nodeId,
                              int attempt, String reason) {
        String resourceId = resourcePool.queryHeldResourceId(executionId, nodeId, attempt);
        if (resourceId == null) return; // 没有资源，跳过

        String holderKey = buildHolderKey(executionId, nodeId, attempt);
        releaseResource(executionId, nodeId, attempt, resourceId, holderKey, reason);
    }

}