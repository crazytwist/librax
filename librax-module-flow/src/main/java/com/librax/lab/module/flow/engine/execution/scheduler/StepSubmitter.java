package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.api.dispatch.DispatchCallback;
import com.librax.lab.module.flow.api.dispatch.DispatchSpi;
import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
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
import java.util.UUID;
import java.util.concurrent.Executor;

import static com.librax.lab.module.flow.engine.execution.scheduler.SchedulerConstants.*;

/**
 * 步骤提交器 — 负责将节点提交给执行器
 *
 * <p>职责：
 * <ol>
 *   <li>CONDITION 节点：同步执行，不走 dispatch_mode
 *   <li>其他节点：组装 {@link StepDispatchContext}，按 dispatch_mode 路由到 {@link DispatchSpi}
 *   <li>处理 WAITING 状态（挂起等待回调）
 *   <li>CONDITION 节点分支跳过处理
 * </ol>
 *
 * <p>已移除：
 * <ul>
 *   <li>{@code executeNode} — 逻辑已下沉到 DirectDispatchSpi，此处不再重复
 *   <li>旧签名 {@code executor.execute(StepNode, executionId, inputParams)} 相关调用
 * </ul>
 */
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

    // ================================================================
    // 对外入口
    // ================================================================

    public void submit(String executionId,
                       PipelineGraph graph,
                       StepNode node,
                       DagSchedulerCallback dagCallback) {

        StepExecutionDO initialStepDO = stepMapper.selectLatestAttempt(
                executionId, node.getNodeId());
        if (initialStepDO == null) {
            log.warn("[StepSubmitter] 步骤记录不存在 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }

        // CONDITION 节点:同步执行,内部自己 tryStart
        if (node.getStepType() == StepTypeEnum.CONDITION) {
            executeCondition(executionId, graph, node, initialStepDO, dagCallback);
            return;
        }

        // ★ 非 CONDITION 节点:在分发前完成 tryStart,生成 callback_token
        boolean acquired = stepStateMachine.tryStart(
                executionId, node.getNodeId(), initialStepDO.getAttempt());
        if (!acquired) {
            log.warn("[StepSubmitter] 步骤已被抢占 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }

        // 重新查一次拿到 tryStart 里生成的 callback_token
        StepExecutionDO stepDO = stepMapper.selectLatestAttempt(executionId, node.getNodeId());


        // 组装上下文,按 dispatch_mode 路由
        StepDispatchContext ctx = buildContext(
                executionId, graph, node, stepDO, dagCallback);

        DispatchSpi spi = dispatchSpiFactory.getSpi(node.getDispatchMode());
        final StepExecutionDO finalStepDO = stepDO;
        stepExecutorPool.execute(() -> {
            try {
                spi.dispatch(ctx);
            } catch (Exception e) {
                log.error("[StepSubmitter] 分发异常 executionId={} nodeId={} error={}",
                        executionId, node.getNodeId(), e.getMessage(), e);
                dagCallback.onStepComplete(executionId, graph,
                        node.getNodeId(), finalStepDO.getAttempt(),
                        StepResult.fail("DISPATCH_ERROR", e.getMessage()));
            }
        });
    }

    // ================================================================
    // CONDITION 节点（同步，不经过 DispatchSpi）
    // ================================================================

    private void executeCondition(String executionId,
                                  PipelineGraph graph,
                                  StepNode node,
                                  StepExecutionDO stepDO,
                                  DagSchedulerCallback dagCallback) {
        int attempt = stepDO.getAttempt();

        // 乐观锁抢占
        boolean acquired = stepStateMachine.tryStart(executionId, node.getNodeId(), attempt);
        if (!acquired) {
            log.warn("[StepSubmitter] CONDITION 步骤已被抢占 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }

        try {
            // 组装 ctx 传给 ConditionStepExecutor（新签名）
            StepDispatchContext ctx = buildContext(
                    executionId, graph, node, stepDO, dagCallback);

            StepExecutor conditionExecutor = executorFactory.getExecutor(StepTypeEnum.CONDITION);
            StepResult result = conditionExecutor.execute(ctx);

            if (result.isSuccess()) {
                markUnmatchedBranchesSkipped(executionId, graph, node, result);
            }

            dagCallback.onStepComplete(executionId, graph,
                    node.getNodeId(), attempt, result);

        } catch (Exception e) {
            log.error("[StepSubmitter] CONDITION 节点异常 executionId={} nodeId={} error={}",
                    executionId, node.getNodeId(), e.getMessage(), e);
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
        String branchName   = (String) result.getOutputs().get("branchName");
        String matchedTarget = (String) result.getOutputs().get("matchedTarget");

        log.info("[StepSubmitter] 条件节点分支选择 executionId={} nodeId={} " +
                        "branchName={} target={}",
                executionId, conditionNode.getNodeId(), branchName, matchedTarget);

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
                                             DagSchedulerCallback dagCallback) {
        Map<String, Object> inputParams = resolveInputParams(executionId, node);

        // callback lambda：把 DagSchedulerCallback 适配成 DispatchCallback
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
                // 基础执行配置
                .stepType(node.getStepType().name())
                .taskType(node.getTaskType())
                .stepKey(node.getStepKey())
                .stepName(node.getName())
                .inputParams(inputParams)
                // 资源调度
                .zoneCode(graph.getZoneCode())
                .priority(0)
                .timeoutMs(node.getTimeoutMs() != null ? node.getTimeoutMs() : 30_000L)
                .maxAttempts(node.getMaxAttempts() != null ? node.getMaxAttempts() : 3)
                // INSTRUMENT 专用
                .deviceType(node.getDeviceType())
                .commandCode(node.getCommand())
                // COMPUTE 专用
                .executor(node.getExecutor())
                .beanName(node.getBeanName())
                .methodName(node.getMethodName())
                .chainId(node.getChainId())
                // CONDITION 专用
                .conditionExpr(node.getConditionExpr())
                .allBranches(node.getAllBranches())
                // MOCK 专用
                .mockOutput(node.getMockOutput())
                // 回调
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
    // 回调接口
    // ================================================================

    public interface DagSchedulerCallback {

        void onStepComplete(String executionId, PipelineGraph graph,
                            String nodeId, int attempt, StepResult result);

        Map<String, Object> resolveInputParams(String executionId, StepNode node);
    }
}