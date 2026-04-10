package com.librax.lab.module.flow.service.pipelineexecution;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.api.PipelineStartHook;
import com.librax.lab.module.flow.dal.dataobject.executioncontext.ExecutionContextDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.executioncontext.ExecutionContextMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.context.ExecutionContextManager;
import com.librax.lab.module.flow.engine.execution.event.ExecutionEventPublisher;
import com.librax.lab.module.flow.engine.execution.scheduler.DagScheduler;
import com.librax.lab.module.flow.engine.execution.statemachine.ExecutionStateMachine;
import com.librax.lab.module.flow.enums.EventTypeEnum;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.librax.lab.module.flow.engine.execution.scheduler.SchedulerConstants.CONTEXT_KEY_INPUT;

import com.librax.lab.module.flow.controller.admin.pipelineexecution.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 流程执行服务实现
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PipelineExecutionServiceImpl implements PipelineExecutionService {

    private final PipelineGraphCache graphCache;
    private final PipelineExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final ExecutionContextMapper contextMapper;
    private final ExecutionStateMachine executionStateMachine;
    private final DagScheduler dagScheduler;
    private final ExecutionContextManager contextManager;
    private final ExecutionEventPublisher eventPublisher;
    // Spring 自动注入所有实现了 PipelineStartHook 的 Bean（来自其他模块）
    private final List<PipelineStartHook> startHooks;



    @Override
    public Long createPipelineExecution(PipelineExecutionSaveReqVO createReqVO) {
        // 插入
        PipelineExecutionDO pipelineExecution = BeanUtils.toBean(createReqVO, PipelineExecutionDO.class);
        executionMapper.insert(pipelineExecution);

        // 返回
        return pipelineExecution.getId();
    }

    @Override
    public void updatePipelineExecution(PipelineExecutionSaveReqVO updateReqVO) {
        // 校验存在
        validatePipelineExecutionExists(updateReqVO.getId());
        // 更新
        PipelineExecutionDO updateObj = BeanUtils.toBean(updateReqVO, PipelineExecutionDO.class);
        executionMapper.updateById(updateObj);
    }

    @Override
    public void deletePipelineExecution(Long id) {
        // 校验存在
        validatePipelineExecutionExists(id);
        // 删除
        executionMapper.deleteById(id);
    }

    @Override
    public void deletePipelineExecutionListByIds(List<Long> ids) {
        // 删除
        executionMapper.deleteByIds(ids);
    }


    private void validatePipelineExecutionExists(Long id) {
        if (executionMapper.selectById(id) == null) {
            throw exception(PIPELINE_EXECUTION_NOT_EXISTS);
        }
    }

    @Override
    public PipelineExecutionDO getPipelineExecution(Long id) {
        return executionMapper.selectById(id);
    }

    @Override
    public PageResult<PipelineExecutionDO> getPipelineExecutionPage(PipelineExecutionPageReqVO pageReqVO) {
        return executionMapper.selectPage(pageReqVO);
    }

    // -----------------------------------------------------------------------------------------------------------------

    // ================================================================
    // start — 启动流程
    // ================================================================

    /**
     * 启动流程，完整步骤：
     * 1. 加载并校验流程定义（从 Cache，含 Validator 校验）
     * 2. 创建 pe_pipeline_execution（PENDING）
     * 3. 初始化所有节点的 pe_step_execution（PENDING）
     * 4. 初始化 pe_execution_context（空）
     * 5. 流程状态 PENDING → RUNNING
     * 6. 触发调度器开始调度
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String start(String pipelineKey,
                        Integer pipelineVersion,
                        Map<String, Object> inputParams,
                        String triggerType,
                        String triggeredBy) {

        // 1. 加载流程图（校验通过才能拿到，否则抛异常）
        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);
        log.info("[ExecutionService] 启动流程 pipelineKey={} version={} triggeredBy={}",
                pipelineKey, graph.getVersion(), triggeredBy);

        // 2. 生成执行实例ID
        String executionId = UUID.randomUUID().toString().replace("-", "");

        // 3. 创建流程执行主记录（PENDING）
        createPipelineExecution(executionId, graph, inputParams, triggerType, triggeredBy);

        // 4. 初始化所有节点的步骤执行记录（PENDING）
        initStepExecutions(executionId, graph);

        // 5. 把流程初始参数写入上下文，供后续节点 ${input.xxx} 引用
        if (inputParams != null && !inputParams.isEmpty()) {
            contextManager.putNodeOutput(executionId, CONTEXT_KEY_INPUT, inputParams);
        }

        // ★ 调用所有注册的前置钩子（lab 模块的 SampleBindHook 在这里执行）
        for (PipelineStartHook hook : startHooks) {
            hook.beforeSchedule(executionId, pipelineKey,
                    graph.getVersion(), inputParams);
        }

        // 6. 流程状态 PENDING → RUNNING（乐观锁）
        executionStateMachine.transition(
                executionId,
                ExecutionStatusEnum.PENDING,
                ExecutionStatusEnum.RUNNING);

        // 7. 触发调度器（事务提交后执行，避免调度器读不到刚插入的数据）
        //    使用 TransactionSynchronizationManager 保证事务提交后再调度

        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                // ★ 新增：发布流程启动事件
                                eventPublisher.publishPipelineEvent(
                                        executionId, null,
                                        EventTypeEnum.PIPELINE_STARTED,
                                        ExecutionStatusEnum.PENDING.name(),
                                        ExecutionStatusEnum.RUNNING.name(),
                                        Map.of("pipelineKey", pipelineKey,
                                                "pipelineVersion", graph.getVersion()));

                                // 触发调度（原有）
                                dagScheduler.schedule(executionId, pipelineKey, graph.getVersion());
                            }
                        });

        log.info("[ExecutionService] 流程已启动 executionId={} pipelineKey={} version={}",
                executionId, pipelineKey, graph.getVersion());
        return executionId;
    }

    // ================================================================
    // pause / resume / cancel
    // ================================================================

    @Override
    public void pause(String executionId) {
        PipelineExecutionDO execution = getExecution(executionId);
        executionStateMachine.transition(
                executionId,
                ExecutionStatusEnum.valueOf(execution.getStatus()),
                ExecutionStatusEnum.PAUSED);
        log.info("[ExecutionService] 流程已暂停 executionId={}", executionId);
    }

    @Override
    public void resume(String executionId) {
        PipelineExecutionDO execution = getExecution(executionId);
        executionStateMachine.transition(
                executionId,
                ExecutionStatusEnum.valueOf(execution.getStatus()),
                ExecutionStatusEnum.RUNNING);

        // 恢复后重新触发调度（从当前状态继续，已完成节点不会重复执行）
        dagScheduler.schedule(
                executionId,
                execution.getPipelineKey(),
                execution.getPipelineVersion());

        log.info("[ExecutionService] 流程已恢复 executionId={}", executionId);
    }

    @Override
    public void cancel(String executionId) {
        PipelineExecutionDO execution = getExecution(executionId);
        executionStateMachine.transition(
                executionId,
                ExecutionStatusEnum.valueOf(execution.getStatus()),
                ExecutionStatusEnum.CANCELLED);
        log.info("[ExecutionService] 流程已取消 executionId={}", executionId);
    }

    // ================================================================
    // 私有方法
    // ================================================================

    /**
     * 创建流程执行主记录
     */
    private void createPipelineExecution(String executionId,
                                         PipelineGraph graph,
                                         Map<String, Object> inputParams,
                                         String triggerType,
                                         String triggeredBy) {
        PipelineExecutionDO record = new PipelineExecutionDO();
        record.setExecutionId(executionId);
        record.setPipelineKey(graph.getPipelineKey());
        record.setPipelineVersion(graph.getVersion());
        record.setStatus(ExecutionStatusEnum.PENDING.name());
        record.setTriggerType(triggerType != null ? triggerType : "MANUAL");
        record.setTriggeredBy(triggeredBy);
        record.setInputParams(inputParams != null ? JSON.toJSONString(inputParams) : null);
        record.setRowVersion(0);
        executionMapper.insert(record);
    }

    /**
     * 初始化所有节点的步骤执行记录（全部 PENDING，attempt=1）
     * queued_at 打点记录进入队列时间，后续可计算排队等待耗时
     */
    private void initStepExecutions(String executionId, PipelineGraph graph) {
        LocalDateTime now = LocalDateTime.now();
        for (StepNode node : graph.getSteps()) {
            StepExecutionDO step = new StepExecutionDO();
            step.setExecutionId(executionId);
            step.setNodeId(node.getNodeId());
            step.setStepKey(node.getStepKey());
            step.setStepType(node.getStepType().name());
            step.setAttempt(1);
            step.setStatus(StepStatusEnum.PENDING.name());
            step.setRunMode("NORMAL");
            step.setQueuedAt(now);
            stepMapper.insert(step);
        }
        log.debug("[ExecutionService] 初始化步骤记录完成 executionId={} count={}",
                executionId, graph.getSteps().size());
    }


    /**
     * 查询执行实例，不存在则抛出业务异常
     */
    private PipelineExecutionDO getExecution(String executionId) {
        PipelineExecutionDO execution = executionMapper.selectByExecutionId(executionId);
        if (execution == null) {
            throw exception(PIPELINE_EXECUTION_NOT_EXISTS, executionId);
        }
        return execution;
    }

}