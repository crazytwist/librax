package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.context.ExecutionContextManager;
import com.librax.lab.module.flow.engine.execution.context.OutputMappingResolver;
import com.librax.lab.module.flow.engine.execution.event.ExecutionEventPublisher;
import com.librax.lab.module.flow.engine.execution.exception.ExceptionEngine;
import com.librax.lab.module.flow.engine.execution.executor.MockStepExecutor;
import com.librax.lab.module.flow.engine.execution.executor.StepExecutor;
import com.librax.lab.module.flow.engine.execution.executor.StepExecutorFactory;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.engine.execution.statemachine.ExecutionStateMachine;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import com.librax.lab.module.flow.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final StepStateMachine stepStateMachine;
    private final ExecutionContextManager contextManager;
    private final StepExecutorFactory executorFactory;
    private final MockStepExecutor mockStepExecutor;  // 无真实执行器时的兜底
    private final ExecutionEventPublisher eventPublisher; // ★ 新增
    private final OutputMappingResolver outputMappingResolver;
    private final ExceptionEngine exceptionEngine;


    // 步骤并行执行线程池（ThreadPoolConfig 里定义的 Bean）
    private final Executor stepExecutorPool;
    // 重试延迟调度线程池
    private final ScheduledExecutorService watchdogPool;

    // ================================================================
    // 对外接口
    // ================================================================

    /**
     * 启动调度（流程刚创建或恢复时调用）
     *
     * @param executionId     执行实例ID
     * @param pipelineKey     流程标识
     * @param pipelineVersion 流程版本
     */
    public void schedule(String executionId,
                         String pipelineKey,
                         Integer pipelineVersion) {
        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);
        doSchedule(executionId, graph);
    }

    /**
     * 节点完成回调（执行器执行完毕后调用，成功或失败都走这里）
     *
     * @param executionId     执行实例ID
     * @param pipelineKey     流程标识
     * @param pipelineVersion 流程版本
     * @param nodeId          完成的节点ID
     * @param attempt         本次尝试次数
     * @param result          执行结果
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
        // 1. 加载该流程所有节点的最新状态（每个 nodeId 取最大 attempt 行）
        List<StepExecutionDO> stepList = stepMapper.selectLatestByExecutionId(executionId);
        Map<String, StepStatusEnum> statusMap = stepList.stream()
                .collect(Collectors.toMap(
                        StepExecutionDO::getNodeId,
                        s -> StepStatusEnum.valueOf(s.getStatus()),
                        (existing, replacement) -> replacement));  // ★ 重复 key 取后者

        // 2. 检查流程是否已全部完成
        if (isAllDone(graph, statusMap)) {
            finishPipeline(executionId, graph, statusMap);
            return;
        }

        // 3. 找出就绪节点：PENDING 且所有前置依赖都已满足
        List<StepNode> readyNodes = graph.getSteps().stream()
                .filter(node -> isPending(node, statusMap))
                .filter(node -> isDependencySatisfied(node, statusMap))
                .collect(Collectors.toList());

        if (readyNodes.isEmpty()) {
            log.debug("[DagScheduler] 无就绪节点 executionId={}", executionId);
            return;
        }

        log.info("[DagScheduler] 就绪节点 executionId={} nodes={}",
                executionId, readyNodes.stream().map(StepNode::getNodeId).collect(Collectors.toList()));

        // 4. 并行提交所有就绪节点
        readyNodes.forEach(node -> submitNode(executionId, graph, node, statusMap));
    }

    // ================================================================
    // 节点提交
    // ================================================================

    private void submitNode(String executionId,
                            PipelineGraph graph,
                            StepNode node,
                            Map<String, StepStatusEnum> statusMap) {
        StepExecutionDO stepDO = stepMapper.selectLatestAttempt(executionId, node.getNodeId());
        if (stepDO == null) {
            log.warn("[DagScheduler] 步骤记录不存在 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }
        int attempt = stepDO.getAttempt();

        // CONDITION 节点：直接在当前线程求值（不需要走线程池）
        if (node.getStepType() == StepTypeEnum.CONDITION) {
            executeConditionNode(executionId, graph, node, attempt);
            return;
        }

        // 其他节点：提交到线程池异步执行
        stepExecutorPool.execute(() -> executeNode(executionId, graph, node, attempt));
    }

    private void executeNode(String executionId,
                             PipelineGraph graph,
                             StepNode node,
                             int attempt) {
        // 1. 乐观锁抢占 PENDING → RUNNING，防止重复提交
        boolean acquired = stepStateMachine.tryStart(executionId, node.getNodeId(), attempt);
        if (!acquired) {
            log.warn("[DagScheduler] 步骤已被其他线程抢占 executionId={} nodeId={}",
                    executionId, node.getNodeId());
            return;
        }

        // 2. 解析 inputMapping，从上下文取前置节点输出
        Map<String, Object> inputParams = resolveInputParams(executionId, node, graph);

        // 3. 调用执行器
        // 没有注册真实执行器时降级到 Mock（开发阶段兜底）
        StepExecutor executor = executorFactory.hasExecutor(node.getStepType())
                ? executorFactory.getExecutor(node.getStepType())
                : mockStepExecutor;

        try {
            StepResult result = executor.execute(node, executionId, inputParams);

            // ★ 新增：异步等待，步骤挂起，等外部回调推进
            if (result.isWaiting()) {
                handleWaiting(executionId, graph, node, attempt, result);
                return;
            }

            // 4. 回调 onStepComplete
            onStepComplete(executionId,
                    graph.getPipelineKey(), graph.getVersion(),
                    node.getNodeId(), attempt, result);

        } catch (Exception e) {
            log.error("[DagScheduler] 节点执行异常 executionId={} nodeId={} error={}",
                    executionId, node.getNodeId(), e.getMessage(), e);
            onStepComplete(executionId,
                    graph.getPipelineKey(), graph.getVersion(),
                    node.getNodeId(), attempt,
                    StepResult.fail("EXECUTE_EXCEPTION", e.getMessage()));
        }
    }


    /**
     * ★ 新增方法：处理 WAITING 状态
     */
    private void handleWaiting(String executionId,
                               PipelineGraph graph,
                               StepNode node,
                               int attempt,
                               StepResult result) {

        // 1. 从执行器返回的 outputs 里取 token
        String callbackToken = result.getOutputs() != null
                ? (String) result.getOutputs().get("_callbackToken")
                : null;
        // 兜底：WAIT（人工审批）等不需要主动发 token 的场景
        if (callbackToken == null) {
            callbackToken = UUID.randomUUID().toString().replace("-", "");
        }

        // 2. 步骤状态 RUNNING → WAITING
        stepStateMachine.markWaiting(
                executionId, node.getNodeId(), attempt,
                result.getWaitingFor(), callbackToken);

        // 3. 把中间数据和令牌写入上下文（供回调时校验）
        Map<String, Object> waitingInfo = new HashMap<>();
        if (result.getOutputs() != null) {
            waitingInfo.putAll(result.getOutputs());
        }
        waitingInfo.put("_callbackToken", callbackToken);
        waitingInfo.put("_waitingFor", result.getWaitingFor().name());
        waitingInfo.put("_waitingSince", LocalDateTime.now().toString());
        contextManager.putNodeOutput(executionId,
                node.getNodeId() + "_waiting", waitingInfo);

        log.info("[DagScheduler] 步骤进入等待 executionId={} nodeId={} waitingFor={} token={}",
                executionId, node.getNodeId(), result.getWaitingFor(), callbackToken);

    }


    /**
     * CONDITION 节点：表达式求值 + 标记未选中分支为 SKIPPED
     * <p>
     * ★ 改动点：从 boolean 二叉分支 改为 string 多路分支
     */
    private void executeConditionNode(String executionId,
                                      PipelineGraph graph,
                                      StepNode node,
                                      int attempt) {
        boolean acquired = stepStateMachine.tryStart(executionId, node.getNodeId(), attempt);
        if (!acquired) return;

        try {
            Map<String, Object> inputParams = resolveInputParams(executionId, node, graph);
            StepResult result = executorFactory
                    .getExecutor(StepTypeEnum.CONDITION)
                    .execute(node, executionId, inputParams);

            if (result.isSuccess()) {
                // ★ 改动：从 outputs 里取 branchName（不再取 conditionResult boolean）
                String branchName = (String) result.getOutputs().get("branchName");
                String matchedTarget = (String) result.getOutputs().get("matchedTarget");

                log.info("[DagScheduler] 条件节点分支选择 executionId={} nodeId={} " +
                                "branchName={} target={}",
                        executionId, node.getNodeId(), branchName, matchedTarget);

                // ★ 改动：标记所有未命中分支为 SKIPPED
                Map<String, String> allBranches = node.getAllBranches();
                for (Map.Entry<String, String> entry : allBranches.entrySet()) {
                    if (!entry.getKey().equals(branchName)
                            && !entry.getValue().equals(matchedTarget)) {
                        // 这条分支未被选中，递归标记 SKIPPED
                        markBranchSkipped(executionId, graph,
                                entry.getValue(), node.getNodeId());
                    }
                }
            }

            onStepComplete(executionId,
                    graph.getPipelineKey(), graph.getVersion(),
                    node.getNodeId(), attempt, result);

        } catch (Exception e) {
            log.error("[DagScheduler] CONDITION节点异常 executionId={} nodeId={} error={}",
                    executionId, node.getNodeId(), e.getMessage(), e);
            onStepComplete(executionId,
                    graph.getPipelineKey(), graph.getVersion(),
                    node.getNodeId(), attempt,
                    StepResult.fail("CONDITION_EVAL_FAIL", e.getMessage()));
        }
    }

    /**
     * 递归标记未选中分支及其所有下游节点为 SKIPPED
     */
    private void markBranchSkipped(String executionId,
                                   PipelineGraph graph,
                                   String nodeId,
                                   String conditionNodeId) {
        if (nodeId == null || !graph.containsNode(nodeId)) return;

        StepExecutionDO stepDO = stepMapper.selectLatestAttempt(executionId, nodeId);
        if (stepDO == null) return;

        StepStatusEnum current = StepStatusEnum.valueOf(stepDO.getStatus());
        // 已经是终态则不再处理（避免重复标记）
        if (current.isTerminal()) return;

        stepStateMachine.markSkipped(executionId, nodeId, stepDO.getAttempt());

        // 递归处理：该节点的下游节点中，dependsOn 只包含已 SKIPPED/SUCCESS 节点的也要标记
        // （仅处理"只依赖被跳过分支"的节点，有其他未完成依赖的节点不处理）
        graph.getSteps().stream()
                .filter(n -> n.getDependsOn() != null && n.getDependsOn().contains(nodeId))
                .filter(n -> !n.getNodeId().equals(conditionNodeId))
                .forEach(n -> {
                    // 该节点的所有依赖都在已跳过或成功的范围内，才递归标记
                    boolean allDepsSkippedOrSuccess = n.getDependsOn().stream()
                            .allMatch(dep -> {
                                StepExecutionDO depDO = stepMapper.selectLatestAttempt(executionId, dep);
                                if (depDO == null) return false;
                                StepStatusEnum s = StepStatusEnum.valueOf(depDO.getStatus());
                                return s == StepStatusEnum.SKIPPED || s == StepStatusEnum.SUCCESS;
                            });
                    if (allDepsSkippedOrSuccess) {
                        markBranchSkipped(executionId, graph, n.getNodeId(), conditionNodeId);
                    }
                });
    }

    // ================================================================
    // 成功处理
    // ================================================================

    private void handleSuccess(String executionId,
                               PipelineGraph graph,
                               StepNode node,
                               int attempt,
                               StepResult result) {
        // 1. 更新步骤状态为 SUCCESS
        stepStateMachine.markSuccess(executionId, node.getNodeId(), attempt, result);

        // 2. 应用 output_mapping，提取并重命名字段
        Map<String, Object> outputs = result.getOutputs();
        if (outputs != null && !outputs.isEmpty()) {
            // ★ 新增：如果配置了 output_mapping，做字段映射
            Map<String, String> outputMapping = node.getOutputMapping();
            Map<String, Object> contextOutputs;

            if (outputMapping != null && !outputMapping.isEmpty()) {
                contextOutputs = outputMappingResolver.resolve(outputs, outputMapping);
                log.info("[DagScheduler] output_mapping 应用完成 nodeId={} raw={} mapped={}",
                        node.getNodeId(), outputs.keySet(), contextOutputs.keySet());
            } else {
                // 没有配置 output_mapping，原样写入
                contextOutputs = outputs;
            }

            // 写入上下文（供后续节点 inputMapping 引用）
            contextManager.putNodeOutput(executionId, node.getNodeId(), contextOutputs);
        }

        // 3. 触发下一轮调度
        doSchedule(executionId, graph);
    }

    // ================================================================
    // 失败处理
    // ================================================================

    private void handleFailure(String executionId,
                               PipelineGraph graph,
                               StepNode node,
                               int attempt,
                               StepResult result) {
        // 1. 标记步骤 FAILED（这一步不变）
        stepStateMachine.markFailed(executionId, node.getNodeId(), attempt, result);

        // 2. ★ 委托给异常引擎处理（替代原来的重试/DEAD判断）
        exceptionEngine.handleStepFailure(
                executionId,
                graph.getPipelineKey(),
                graph.getVersion(),
                node.getNodeId(),
                attempt,
                result.getErrorCode(),
                result.getErrorMsg());
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

        // ★ 新增：发布流程完成事件
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

    /**
     * 节点是否处于 PENDING 状态
     */
    private boolean isPending(StepNode node, Map<String, StepStatusEnum> statusMap) {
        StepStatusEnum status = statusMap.get(node.getNodeId());
        return status == StepStatusEnum.PENDING;
    }

    /**
     * 节点的所有前置依赖是否都已满足（SUCCESS 或 SKIPPED）
     */
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
    private Map<String, Object> resolveInputParams(String executionId,
                                                   StepNode node,
                                                   PipelineGraph graph) {
        // 取流程初始参数（input_params）
        Map<String, Object> inputParams = contextManager
                .getNodeOutput(executionId, "input");

        // inputMapping 解析（${s_ph.ph}、${input.sampleId} 等表达式）
        Map<String, Object> mappedParams = contextManager.resolveInputMapping(
                executionId, node.getInputMapping(), inputParams);

        // 合并：node.params（静态参数）+ mappedParams（动态参数，优先级更高）
        Map<String, Object> merged = new HashMap<>(node.getParams());
        if (mappedParams != null) {
            merged.putAll(mappedParams);
        }
        return merged;
    }

    /**
     * 延迟调度（供 ExceptionEngine 重试时调用）
     */
    public void scheduleDelayed(String executionId, PipelineGraph graph, long delayMs) {
        watchdogPool.schedule(
                () -> doSchedule(executionId, graph),
                delayMs,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 立即调度（供 ExceptionEngine CONTINUE_ON_FAIL 时调用）
     */
    public void scheduleImmediate(String executionId, PipelineGraph graph) {
        doSchedule(executionId, graph);
    }
}