package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.api.task.ManualTaskRequest;
import com.librax.lab.module.flow.api.task.ManualTaskSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MANUAL 步骤执行器
 * <p>
 * 将 flow 引擎的 MANUAL 步骤桥接到 task 模块：
 * <ol>
 *   <li>从步骤参数中提取人工任务配置（assignee / formId / dueHours / waitReason）
 *   <li>通过 {@link ManualTaskSpi} 创建 lab_task 记录并推入 MANUAL 队列
 *   <li>返回 {@link StepResult#waitForApproval} 通知 DirectDispatchSpi 将步骤标记为等待
 * </ol>
 * <p>
 * 操作人在待办系统完成任务后，携带 callbackToken 调用回调接口，
 * 引擎收到回调后将步骤推进为成功并继续 DAG 调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualStepExecutor implements StepExecutor {

    private final ManualTaskSpi manualTaskSpi;

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.MANUAL;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        Map<String, Object> params = ctx.getInputParams();

        String assignee  = getString(params, "assignee", "");
        String formId    = getString(params, "formId", null);
        String waitReason = getString(params, "waitReason", "等待人工处理");
        int dueHours     = getInt(params, "dueHours", 24);

        log.info("[ManualStepExecutor] 创建人工任务 executionId={} nodeId={} assignee={} formId={}",
                ctx.getExecutionId(), ctx.getNodeId(), assignee, formId);

        String taskId = manualTaskSpi.submitTask(ManualTaskRequest.builder()
                .executionId(ctx.getExecutionId())
                .nodeId(ctx.getNodeId())
                .stepKey(ctx.getStepKey())
                .stepName(ctx.getStepName())
                .callbackToken(ctx.getCallbackToken())
                .attempt(ctx.getAttempt())
                .assignee(assignee)
                .formId(formId)
                .dueHours(dueHours)
                .waitReason(waitReason)
                .priority(ctx.getPriority())
                .zoneCode(ctx.getZoneCode())
                .build());

        log.info("[ManualStepExecutor] 人工任务已创建 taskId={} executionId={} nodeId={}",
                taskId, ctx.getExecutionId(), ctx.getNodeId());

        return StepResult.waitForApproval(Map.of(
                "taskId",     taskId,
                "waitReason", waitReason,
                "assignee",   assignee
        ));
    }

    private String getString(Map<String, Object> params, String key, String defaultVal) {
        Object val = params.get(key);
        return val instanceof String s ? s : defaultVal;
    }

    private int getInt(Map<String, Object> params, String key, int defaultVal) {
        Object val = params.get(key);
        return val instanceof Number n ? n.intValue() : defaultVal;
    }
}
