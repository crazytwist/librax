package com.librax.lab.module.flow.engine.standalone.service;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.context.ExecutionContextManager;
import com.librax.lab.module.flow.engine.execution.executor.MockStepExecutor;
import com.librax.lab.module.flow.engine.execution.executor.StepExecutor;
import com.librax.lab.module.flow.engine.execution.executor.StepExecutorFactory;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.engine.execution.statemachine.ExecutionStateMachine;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import com.librax.lab.module.flow.engine.standalone.vo.StandaloneRunReqVO;
import com.librax.lab.module.flow.engine.standalone.vo.StandaloneRunResultVO;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 单独运行服务实现
 *
 * <p>执行流程：
 * <ol>
 *   <li>校验：加载流程图，确认节点存在且 runnableStandalone=true
 *   <li>创建 execution 记录（trigger_type=STANDALONE）
 *   <li>只初始化目标节点的 step_execution（不初始化其他节点）
 *   <li>注入 mock 上下文（模拟前置节点输出 + input_params）
 *   <li>流程 PENDING → RUNNING
 *   <li>直接执行目标节点（不走 DagScheduler 的依赖检查）
 *   <li>同步节点：等结果返回 → 流程进入终态
 *   <li>异步节点：返回 WAITING → 等外部回调（通过 StepCallbackService 推进）
 * </ol>
 *
 * <p>设计决策：
 * <ul>
 *   <li>复用 StepExecutor / StepStateMachine / ExecutionContextManager 等现有基础设施
 *   <li>不走 DagScheduler.doSchedule()（因为只有一个节点，不需要 DAG 遍历和依赖检查）
 *   <li>直接调用执行器，但状态流转走正规的 StateMachine（保持事件日志完整）
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StandaloneExecutionServiceImpl implements StandaloneExecutionService {

    private final PipelineGraphCache graphCache;
    private final PipelineExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final ExecutionStateMachine executionStateMachine;
    private final StepStateMachine stepStateMachine;
    private final ExecutionContextManager contextManager;
    private final StepExecutorFactory executorFactory;
    private final MockStepExecutor mockStepExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StandaloneRunResultVO run(StandaloneRunReqVO req) {

        // 1. 加载流程图 + 校验节点
        PipelineGraph graph = graphCache.get(req.getPipelineKey(), req.getPipelineVersion());
        StepNode node = graph.getStep(req.getNodeId());

        if (!node.isRunnableStandalone()) {
            throw new RuntimeException(
                    "节点不支持单独运行: " + req.getNodeId());
        }

        log.info("[Standalone] 开始单独运行 pipelineKey={} version={} nodeId={} triggeredBy={}",
                req.getPipelineKey(), req.getPipelineVersion(),
                req.getNodeId(), req.getTriggeredBy());

        // 2. 创建 execution 记录
        String executionId = UUID.randomUUID().toString().replace("-", "");
        createExecutionRecord(executionId, graph, node, req);

        // 3. 只初始化目标节点的 step_execution
        initTargetStepExecution(executionId, node);

        // 4. 注入上下文
        injectContext(executionId, req, graph, node);

        // 5. PENDING → RUNNING
        executionStateMachine.transition(
                executionId,
                ExecutionStatusEnum.PENDING,
                ExecutionStatusEnum.RUNNING);

        // 6. 执行节点（事务提交后同步执行）
        return executeTargetNode(executionId, graph, node);
    }

    // ================================================================
    // 创建执行记录
    // ================================================================

    private void createExecutionRecord(String executionId,
                                       PipelineGraph graph,
                                       StepNode node,
                                       StandaloneRunReqVO req) {
        PipelineExecutionDO record = new PipelineExecutionDO();
        record.setExecutionId(executionId);
        record.setPipelineKey(graph.getPipelineKey());
        record.setPipelineVersion(graph.getVersion());
        record.setStatus(ExecutionStatusEnum.PENDING.name());
        record.setTriggerType("STANDALONE");
        record.setTriggeredBy(req.getTriggeredBy());
        record.setStandaloneNodeId(node.getNodeId());
        record.setParentExecutionId(req.getParentExecutionId());
        record.setInputParams(
                req.getInputParams() != null
                        ? JSON.toJSONString(req.getInputParams()) : null);
        record.setRowVersion(0);
        executionMapper.insert(record);
    }

    // ================================================================
    // 初始化目标节点（只这一个，不初始化其他节点）
    // ================================================================

    private void initTargetStepExecution(String executionId, StepNode node) {
        StepExecutionDO step = new StepExecutionDO();
        step.setExecutionId(executionId);
        step.setNodeId(node.getNodeId());
        step.setStepKey(node.getStepKey());
        step.setStepType(node.getStepType().name());
        step.setAttempt(1);
        step.setStatus(StepStatusEnum.PENDING.name());
        step.setRunMode("STANDALONE");
        step.setQueuedAt(LocalDateTime.now());
        stepMapper.insert(step);
    }

    // ================================================================
    // 注入上下文
    // ================================================================

    private void injectContext(String executionId,
                               StandaloneRunReqVO req,
                               PipelineGraph graph,
                               StepNode node) {

        // 优先级1：如果有 parentExecutionId，从父执行拉取上下文
        if (req.getParentExecutionId() != null) {
            Map<String, Map<String, Object>> parentContext =
                    contextManager.getAllOutputs(req.getParentExecutionId());
            if (!parentContext.isEmpty()) {
                parentContext.forEach((nodeId, outputs) ->
                        contextManager.putNodeOutput(executionId, nodeId, outputs));
                log.info("[Standalone] 从父执行继承上下文 parentExecutionId={} keys={}",
                        req.getParentExecutionId(), parentContext.keySet());
            }
        }

        // 优先级2：用户手动提供的 mockContext（覆盖父执行的同名 key）
        if (req.getMockContext() != null && !req.getMockContext().isEmpty()) {
            req.getMockContext().forEach((nodeId, outputs) ->
                    contextManager.putNodeOutput(executionId, nodeId, outputs));
            log.info("[Standalone] 注入 mockContext keys={}",
                    req.getMockContext().keySet());
        }

        // 优先级3：写入 input 参数
        if (req.getInputParams() != null && !req.getInputParams().isEmpty()) {
            contextManager.putNodeOutput(executionId, "input", req.getInputParams());
        }
    }

    // ================================================================
    // 执行目标节点
    // ================================================================

    private StandaloneRunResultVO executeTargetNode(String executionId,
                                                    PipelineGraph graph,
                                                    StepNode node) {
        // 1. 抢锁 PENDING → RUNNING
        boolean acquired = stepStateMachine.tryStart(
                executionId, node.getNodeId(), 1);
        if (!acquired) {
            return StandaloneRunResultVO.syncFailed(
                    executionId, "LOCK_FAILED", "步骤抢锁失败");
        }

        // 2. 解析入参
        Map<String, Object> inputParams = resolveInputParams(executionId, node);

        // 3. 选择执行器
        StepExecutor executor = executorFactory.hasExecutor(node.getStepType())
                ? executorFactory.getExecutor(node.getStepType())
                : mockStepExecutor;

        try {
            // 4. 执行
            StepResult result = executor.execute(node, executionId, inputParams);

            // 5. 处理结果
            if (result.isWaiting()) {
                // 异步节点：标记 WAITING，返回 executionId 让前端等回调
                handleWaiting(executionId, node, result);
                return StandaloneRunResultVO.async(executionId);
            }

            if (result.isSuccess()) {
                // 同步成功
                stepStateMachine.markSuccess(
                        executionId, node.getNodeId(), 1, result);
                if (result.getOutputs() != null && !result.getOutputs().isEmpty()) {
                    contextManager.putNodeOutput(
                            executionId, node.getNodeId(), result.getOutputs());
                }
                finishExecution(executionId, ExecutionStatusEnum.SUCCESS);
                return StandaloneRunResultVO.syncSuccess(
                        executionId, result.getOutputs());
            } else {
                // 同步失败
                stepStateMachine.markFailed(
                        executionId, node.getNodeId(), 1, result);
                stepStateMachine.markDead(
                        executionId, node.getNodeId(), 1);
                finishExecution(executionId, ExecutionStatusEnum.FAILED);
                return StandaloneRunResultVO.syncFailed(
                        executionId, result.getErrorCode(), result.getErrorMsg());
            }

        } catch (Exception e) {
            log.error("[Standalone] 执行异常 executionId={} nodeId={} error={}",
                    executionId, node.getNodeId(), e.getMessage(), e);
            StepResult failResult = StepResult.fail(
                    "EXECUTE_EXCEPTION", e.getMessage());
            stepStateMachine.markFailed(
                    executionId, node.getNodeId(), 1, failResult);
            stepStateMachine.markDead(
                    executionId, node.getNodeId(), 1);
            finishExecution(executionId, ExecutionStatusEnum.FAILED);
            return StandaloneRunResultVO.syncFailed(
                    executionId, "EXECUTE_EXCEPTION", e.getMessage());
        }
    }

    // ================================================================
    // 异步节点处理（复用 DagScheduler 的 WAITING 逻辑）
    // ================================================================

    private void handleWaiting(String executionId,
                               StepNode node,
                               StepResult result) {
        String callbackToken = result.getOutputs() != null
                ? (String) result.getOutputs().get("_callbackToken")
                : null;
        if (callbackToken == null) {
            callbackToken = UUID.randomUUID().toString().replace("-", "");
        }

        stepStateMachine.markWaiting(
                executionId, node.getNodeId(), 1,
                result.getWaitingFor(), callbackToken);

        Map<String, Object> waitingInfo = new HashMap<>();
        if (result.getOutputs() != null) {
            waitingInfo.putAll(result.getOutputs());
        }
        waitingInfo.put("_callbackToken", callbackToken);
        waitingInfo.put("_waitingFor", result.getWaitingFor().name());
        waitingInfo.put("_waitingSince", LocalDateTime.now().toString());
        contextManager.putNodeOutput(
                executionId, node.getNodeId() + "_waiting", waitingInfo);

        log.info("[Standalone] 节点进入等待 executionId={} nodeId={} waitingFor={}",
                executionId, node.getNodeId(), result.getWaitingFor());
    }

    // ================================================================
    // 流程终态
    // ================================================================

    private void finishExecution(String executionId,
                                 ExecutionStatusEnum finalStatus) {
        executionStateMachine.transition(
                executionId,
                ExecutionStatusEnum.RUNNING,
                finalStatus);
        if (finalStatus == ExecutionStatusEnum.SUCCESS) {
            contextManager.cleanup(executionId);
        }
    }

    // ================================================================
    // 入参解析（复用 DagScheduler 的逻辑）
    // ================================================================

    private Map<String, Object> resolveInputParams(String executionId,
                                                   StepNode node) {
        Map<String, Object> inputParams = contextManager
                .getNodeOutput(executionId, "input");

        Map<String, Object> mappedParams = contextManager.resolveInputMapping(
                executionId, node.getInputMapping(), inputParams);

        Map<String, Object> merged = new HashMap<>(
                node.getParams() != null ? node.getParams() : Map.of());
        if (mappedParams != null) {
            merged.putAll(mappedParams);
        }
        return merged;
    }
}
