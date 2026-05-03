package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.context.ExecutionContextManager;
import com.librax.lab.module.flow.engine.execution.event.ExecutionEventPublisher;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.engine.execution.statemachine.ExecutionStateMachine;
import com.librax.lab.module.flow.enums.*;
import com.librax.lab.module.infra.mdc.ExecutionMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.librax.lab.module.flow.api.scheduler.SchedulerConstants.CONTEXT_KEY_INPUT;

/**
 * DAG 调度器 — 核心大脑
 *
 * <p>职责：
 * <ol>
 *   <li>分析当前哪些节点就绪（dependsOn 全部 SUCCESS/SKIPPED）
 *   <li>并行提交就绪节点给执行器
 *   <li>处理节点完成回调，触发下一轮调度
 *   <li>处理重试、DEAD、流程终态
 * </ol>
 *
 * <p>调度触发时机：
 * <ul>
 *   <li>流程启动时（start）
 *   <li>任意节点进入终态时（onStepComplete）
 *   <li>流程恢复时（resume）
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DagScheduler {

    private final PipelineGraphCache graphCache;
    private final StepExecutionMapper stepMapper;
    private final ExecutionStateMachine executionStateMachine;
    private final ExecutionContextManager contextManager;
    private final ExecutionEventPublisher eventPublisher;
    private final ScheduledExecutorService watchdogPool;

    // 委托组件
    private final StepSubmitter stepSubmitter;
    private final StepSuccessHandler successHandler;
    private final StepFailureHandler failureHandler;

    // ================================================================
    // 对外接口
    // ================================================================

    /**
     * 启动调度（流程刚创建或恢复时调用）
     */
    public void schedule(String executionId,
                         String pipelineKey,
                         Integer pipelineVersion) {
        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);
        doSchedule(executionId, graph);
    }

    /**
     * 节点完成回调（执行器执行完毕后调用，成功或失败都走这里）
     */
    public void onStepComplete(String executionId,
                               String pipelineKey,
                               Integer pipelineVersion,
                               String nodeId,
                               int attempt,
                               StepResult result) {

        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);
        StepNode node = graph.getStep(nodeId);
        if (result.isSuccess()) {
            handleSuccess(executionId, graph, node, attempt, result);
        } else {
            handleFailure(executionId, graph, node, attempt, result);
        }
    }

    // ================================================================
    // 核心调度逻辑
    // ================================================================

    /**
     * 调度核心：加载所有步骤当前状态，找出就绪节点并行提交
     */
    private void doSchedule(String executionId, PipelineGraph graph) {
        ExecutionMdc.set(executionId);
        try {
            // 1. 加载该流程所有节点的最新状态
            List<StepExecutionDO> stepList = stepMapper.selectLatestByExecutionId(executionId);
            Map<String, StepStatusEnum> statusMap = stepList.stream()
                    .collect(Collectors.toMap(
                            StepExecutionDO::getNodeId,
                            s -> StepStatusEnum.valueOf(s.getStatus()),
                            (existing, replacement) -> replacement));

            // 2. 检查流程是否已全部完成
            if (isAllDone(graph, statusMap)) {
                finishPipeline(executionId, graph, statusMap);
                return;
            }

            // 3. 找出就绪节点：PENDING 且所有前置依赖都已满足
            List<StepNode> readyNodes = graph.getSteps().stream()
                    .filter(node -> isPending(node, statusMap))
                    .filter(node -> isDependencySatisfied(node, statusMap))
                    .toList();

            if (readyNodes.isEmpty()) {
                log.debug("[DagScheduler] 无就绪节点 executionId={}", executionId);
                return;
            }

            log.info("[DagScheduler] 就绪节点 executionId={} nodes={}",
                    executionId, readyNodes.stream().map(StepNode::getNodeId).collect(Collectors.toList()));

            // 4. 并行提交所有就绪节点
            readyNodes.forEach(node -> stepSubmitter.submit(executionId, graph, node,
                    new StepSubmitter.DagSchedulerCallback() {
                        @Override
                        public void onStepComplete(String execId, PipelineGraph g,
                                                   String nodeId, int attempt, StepResult result) {
                            DagScheduler.this.onStepComplete(execId, g.getPipelineKey(),
                                    g.getVersion(), nodeId, attempt, result);
                        }

                        @Override
                        public Map<String, Object> resolveInputParams(String execId, StepNode n) {
                            return DagScheduler.this.resolveInputParams(execId, n);
                        }
                    }));
        } finally {
            ExecutionMdc.clear();
        }
    }

    // ================================================================
    // 成功/失败处理（委托给组件）
    // ================================================================

    private void handleSuccess(String executionId,
                               PipelineGraph graph,
                               StepNode node,
                               int attempt,
                               StepResult result) {
        successHandler.handle(executionId, graph, node, attempt, result,
                () -> doSchedule(executionId, graph));
    }

    private void handleFailure(String executionId,
                               PipelineGraph graph,
                               StepNode node,
                               int attempt,
                               StepResult result) {
        failureHandler.handle(executionId, graph, node, attempt, result,
                delayMs -> scheduleDelayed(executionId, graph, delayMs),
                () -> doSchedule(executionId, graph));
    }

    // ================================================================
    // 流程完成判断
    // ================================================================

    /**
     * 判断所有节点是否都已进入终态
     */
    private boolean isAllDone(PipelineGraph graph,
                              Map<String, StepStatusEnum> statusMap) {
        return graph.getSteps().stream().allMatch(node -> {
            StepStatusEnum status = statusMap.get(node.getNodeId());
            return status != null && status.isTerminal();
        });
    }

    /**
     * 流程结束处理：根据是否有 DEAD 节点决定流程终态
     */
    private void finishPipeline(String executionId,
                                PipelineGraph graph,
                                Map<String, StepStatusEnum> statusMap) {
        boolean hasDead = statusMap.values().stream()
                .anyMatch(s -> s == StepStatusEnum.DEAD);

        ExecutionStatusEnum finalStatus = hasDead
                ? ExecutionStatusEnum.FAILED
                : ExecutionStatusEnum.SUCCESS;

        log.info("[DagScheduler] 流程结束 executionId={} status={}", executionId, finalStatus);

        executionStateMachine.transition(
                executionId,
                ExecutionStatusEnum.RUNNING,
                finalStatus);

        eventPublisher.publishPipelineEvent(
                executionId, null,
                hasDead ? EventTypeEnum.PIPELINE_FAILED : EventTypeEnum.PIPELINE_SUCCESS,
                ExecutionStatusEnum.RUNNING.name(),
                finalStatus.name(),
                null);

        if (finalStatus == ExecutionStatusEnum.SUCCESS) {
            contextManager.cleanup(executionId);
        }
    }

    // ================================================================
    // 工具方法
    // ================================================================

    private boolean isPending(StepNode node, Map<String, StepStatusEnum> statusMap) {
        StepStatusEnum status = statusMap.get(node.getNodeId());
        return status == StepStatusEnum.PENDING;
    }

    private boolean isDependencySatisfied(StepNode node,
                                          Map<String, StepStatusEnum> statusMap) {
        if (node.getDependsOn() == null || node.getDependsOn().isEmpty()) return true;
        return node.getDependsOn().stream().allMatch(dep -> {
            StepStatusEnum depStatus = statusMap.get(dep);
            return depStatus != null && depStatus.isDependencySatisfied();
        });
    }

    /**
     * 解析节点入参：合并 params + inputMapping 解析结果
     */
    private Map<String, Object> resolveInputParams(String executionId, StepNode node) {
        Map<String, Object> inputParams = contextManager
                .getNodeOutput(executionId, CONTEXT_KEY_INPUT);

        Map<String, Object> mappedParams = contextManager.resolveInputMapping(
                executionId, node.getInputMapping(), inputParams);

        Map<String, Object> merged = new HashMap<>(node.getParams());
        if (mappedParams != null) {
            merged.putAll(mappedParams);
        }
        return merged;
    }

    /**
     * 延迟调度（供重试时调用）
     */
    public void scheduleDelayed(String executionId, PipelineGraph graph, long delayMs) {
        watchdogPool.schedule(
                ExecutionMdc.wrap(() -> doSchedule(executionId, graph),
                        executionId, null, 0),
                delayMs,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 立即调度（供 DEAD_CONTINUE 时调用）
     */
    public void scheduleImmediate(String executionId, PipelineGraph graph) {
        doSchedule(executionId, graph);
    }

}
