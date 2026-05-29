package com.librax.lab.module.task.dispatch;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.api.dispatch.DispatchSpi;
import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.WaitingForEnum;
import com.librax.lab.module.flow.api.statemachine.StepStateApi;
import com.librax.lab.module.infra.framework.util.LabIdGenerator;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.enums.TaskStatusEnum;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import com.librax.lab.module.task.executor.TaskRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueuedDispatchSpi implements DispatchSpi {

    private final TaskMapper taskMapper;
    private final TaskRouter taskRouter;
    private final LabIdGenerator idGenerator;
    private final StepStateApi stepStateApi;


    @Override
    public String supportMode() {
        return "QUEUED";
    }


    @Override
    public void dispatch(StepDispatchContext ctx) {
        TaskDO task = buildTask(ctx);
        taskMapper.insert(task);
        taskRouter.enqueue(task);

        stepStateApi.markWaiting(
                ctx.getExecutionId(),
                ctx.getNodeId(),
                ctx.getAttempt(),
                resolveWaitingFor(ctx),
                ctx.getCallbackToken());

        log.info("[QueuedDispatchSpi] 任务已入队并标记WAITING taskId={} nodeId={} type={}",
                task.getTaskId(), ctx.getNodeId(), ctx.getStepType());
    }

    private TaskDO buildTask(StepDispatchContext ctx) {
        TaskDO task = new TaskDO();
        task.setTaskId(idGenerator.nextTaskId());
        task.setTaskType(resolveTaskType(ctx));
        task.setTaskName(ctx.getStepName());
        task.setExecutionId(ctx.getExecutionId());
        task.setNodeId(ctx.getNodeId());
        task.setStepKey(ctx.getStepKey());
        task.setZoneCode(ctx.getZoneCode());
        task.setPriority(ctx.getPriority());
        task.setCallbackToken(ctx.getCallbackToken());
        task.setStatus(TaskStatusEnum.PENDING.name());
        task.setRetryCount(0);
        task.setMaxRetry(ctx.getMaxAttempts());
        task.setQueuedAt(LocalDateTime.now());
        task.setPayload(buildPayload(ctx));
        task.setAttempt(ctx.getAttempt());
        return task;
    }

    private String buildPayload(StepDispatchContext ctx) {
        Map<String, Object> payload = new HashMap<>();
        // 通用字段
        payload.put("stepType", ctx.getStepType());
        payload.put("inputParams", ctx.getInputParams());
        // INSTRUMENT
        payload.put("deviceType", ctx.getDeviceType());
        payload.put("commandCode", ctx.getCommandCode());
        // COMPUTE
        payload.put("executor", ctx.getExecutor());
        payload.put("beanName", ctx.getBeanName());
        payload.put("methodName", ctx.getMethodName());
        payload.put("chainId", ctx.getChainId());
        // CONDITION
        payload.put("conditionExpr", ctx.getConditionExpr());
        payload.put("allBranches", ctx.getAllBranches());
        return JSON.toJSONString(payload);
    }

    /**
     * 解析任务执行类型
     * 优先用 ctx.taskType(前端配置),为空时按 stepType 兜底
     */
    private String resolveTaskType(StepDispatchContext ctx) {
        // 1. 显式指定优先
        if (ctx.getTaskType() != null && !ctx.getTaskType().isEmpty()) {
            return ctx.getTaskType();
        }
        // 2. 按 stepType 兜底推断
        //    INSTRUMENT 步骤进队列时,task_type 默认也是 INSTRUMENT
        //    其他类型一一对应
        return ctx.getStepType();
    }

    private WaitingForEnum resolveWaitingFor(StepDispatchContext ctx) {
        String taskType = resolveTaskType(ctx);
        return switch (taskType) {
            case "AGV" -> WaitingForEnum.DEVICE_CALLBACK;   // AGV 也算设备回调
            case "MANUAL" -> WaitingForEnum.MANUAL_APPROVE;
            case "INSTRUMENT" -> WaitingForEnum.DEVICE_CALLBACK;
            default -> WaitingForEnum.TASK;
        };
    }
}