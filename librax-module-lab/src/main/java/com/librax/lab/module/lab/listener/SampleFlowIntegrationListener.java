package com.librax.lab.module.lab.listener;


import com.librax.lab.module.flow.engine.execution.context.ExecutionContextManager;
import com.librax.lab.module.flow.engine.execution.event.*;
import com.librax.lab.module.flow.engine.execution.scheduler.SchedulerConstants;
import com.librax.lab.module.lab.service.sample.SampleLifecycleService;
import com.librax.lab.module.lab.service.sample.SampleResultHandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.librax.lab.module.flow.enums.StepTypeEnum.INSTRUMENT;
import static com.librax.lab.module.flow.engine.execution.scheduler.SchedulerConstants.CONTEXT_KEY_INPUT;

/**
 * 样本-流程集成监听器
 * <p>
 * 监听 flow 模块发出的 Spring Event，翻译成样本域的操作。
 * 这是 lab 模块和 flow 模块的唯一集成点。
 * <p>
 * 所有方法都是 @Async 的，不阻塞流程引擎主链路。
 * 样本域操作失败不影响流程执行（降级为日志记录）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleFlowIntegrationListener {

    private final SampleLifecycleService sampleService;
    private final ExecutionContextManager contextManager;
    private final SampleResultHandleService resultHandleService;

    /**
     * 流程启动 → 样本进入流程
     */
    @Async("labEventListenerExecutor")
    @EventListener
    public void onExecutionStarted(ExecutionStartedEvent event) {
        try {
            List<String> sampleIds = resolveSampleIds(event.getExecutionId());
            if (sampleIds.isEmpty()) return;

            log.info("[SampleIntegration] 流程启动，处理样本 executionId={} samples={}",
                    event.getExecutionId(), sampleIds);

        } catch (Exception e) {
            log.error("[SampleIntegration] 流程启动样本处理异常 executionId={}",
                    event.getExecutionId(), e);
        }
    }

    /**
     * 步骤开始 → 更新样本处理状态
     * <p>
     * 只处理 INSTRUMENT 类型的步骤，其他步骤不涉及样本
     */
    @Async("labEventListenerExecutor")
    @EventListener
    public void onStepStarted(StepStartedEvent event) {
        try {
            // 只处理 INSTRUMENT 步骤
            if (!"INSTRUMENT".equals(event.getStepType())) return;

            List<String> sampleIds = resolveSampleIds(event.getExecutionId());
            if (sampleIds.isEmpty()) return;

            for (String sampleId : sampleIds) {
                sampleService.onStepStarted(
                        sampleId,
                        event.getExecutionId(),
                        event.getNodeId(),
                        event.getAttempt());
            }
        } catch (Exception e) {
            log.error("[SampleIntegration] 步骤开始样本处理异常 executionId={} nodeId={}",
                    event.getExecutionId(), event.getNodeId(), e);
        }
    }

    /**
     * 步骤成功 → 记录检测结果
     */
    @Async("labEventListenerExecutor")
    @EventListener
    public void onStepSuccess(StepSuccessEvent event) {
        try {
            if (!INSTRUMENT.name().equals(event.getStepType())) return;

            List<String> sampleIds = resolveSampleIds(event.getExecutionId());
            if (sampleIds.isEmpty()) return;

            // 从上下文取步骤输出
            Map<String, Object> outputs = contextManager
                    .getNodeOutput(event.getExecutionId(), event.getNodeId());

            for (String sampleId : sampleIds) {

                sampleService.onStepCompleted(
                        sampleId,
                        event.getExecutionId(),
                        event.getNodeId(),
                        event.getAttempt(),
                        outputs);

                resultHandleService.saveResultsFromOutput(
                        sampleId,
                        event.getExecutionId(),
                        event.getNodeId(),
                        outputs);
            }
        } catch (Exception e) {
            log.error("[SampleIntegration] 步骤成功样本处理异常 executionId={} nodeId={}",
                    event.getExecutionId(), event.getNodeId(), e);
        }
    }

    /**
     * 步骤失败 → 标记样本该步骤失败
     */
    @Async("labEventListenerExecutor")
    @EventListener
    public void onStepFailed(StepFailedEvent event) {
        try {
            if (!"INSTRUMENT".equals(event.getStepType())) return;

            List<String> sampleIds = resolveSampleIds(event.getExecutionId());
            if (sampleIds.isEmpty()) return;

            for (String sampleId : sampleIds) {
                sampleService.onStepFailed(
                        sampleId,
                        event.getExecutionId(),
                        event.getNodeId(),
                        event.getAttempt());
            }
        } catch (Exception e) {
            log.error("[SampleIntegration] 步骤失败样本处理异常 executionId={} nodeId={}",
                    event.getExecutionId(), event.getNodeId(), e);
        }
    }

    /**
     * 流程结束 → 样本结算
     */
    @Async("labEventListenerExecutor")
    @EventListener
    public void onExecutionCompleted(ExecutionCompletedEvent event) {
        try {
            List<String> sampleIds = resolveSampleIds(event.getExecutionId());
            if (sampleIds.isEmpty()) return;

            log.info("[SampleIntegration] 流程结束，结算样本 executionId={} success={} samples={}",
                    event.getExecutionId(), event.isSuccess(), sampleIds);

            for (String sampleId : sampleIds) {
                sampleService.onExecutionCompleted(
                        sampleId,
                        event.getExecutionId(),
                        event.isSuccess());
            }
        } catch (Exception e) {
            log.error("[SampleIntegration] 流程结束样本处理异常 executionId={}",
                    event.getExecutionId(), e);
        }
    }

    // ================================================================
    // 工具方法
    // ================================================================

    /**
     * 从流程上下文的 input 参数中获取样本ID列表
     * <p>
     * 支持两种传参方式：
     * input_params.sampleId = "S001"                    → 单样本
     * input_params.sampleIds = ["S001", "S002", "S003"] → 批量
     */
    private List<String> resolveSampleIds(String executionId) {
        Map<String, Object> input = contextManager
                .getNodeOutput(executionId, CONTEXT_KEY_INPUT);
        if (input == null) return List.of();

        // 优先取批量
        Object sampleIds = input.get("sampleIds");
        if (sampleIds instanceof List) {
            return ((List<?>) sampleIds).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        // 单样本
        Object sampleId = input.get("sampleId");
        if (sampleId != null) {
            return List.of(sampleId.toString());
        }

        return List.of();
    }
}
