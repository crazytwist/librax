package com.librax.lab.module.task.spi;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.api.task.ManualTaskRequest;
import com.librax.lab.module.flow.api.task.ManualTaskSpi;
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
import java.util.Map;

/**
 * ManualTaskSpi 实现
 * <p>
 * 将 flow-api 定义的接口落地到 task 模块：
 * 创建 lab_task 记录（task_type=MANUAL）并推入 TaskRouter，
 * TaskRouter 调用 {@link com.librax.lab.module.task.executor.ManualTaskExecutor}
 * 在待办系统创建工单，等操作人提交后通过 callbackToken 回调引擎。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualTaskSpiImpl implements ManualTaskSpi {

    private final TaskMapper taskMapper;
    private final TaskRouter taskRouter;
    private final LabIdGenerator idGenerator;

    @Override
    public String submitTask(ManualTaskRequest req) {
        TaskDO task = buildTask(req);
        taskMapper.insert(task);
        taskRouter.enqueue(task);

        log.info("[ManualTaskSpiImpl] 人工任务已入队 taskId={} executionId={} nodeId={} assignee={}",
                task.getTaskId(), req.getExecutionId(), req.getNodeId(), req.getAssignee());

        return task.getTaskId();
    }

    private TaskDO buildTask(ManualTaskRequest req) {
        LocalDateTime now = LocalDateTime.now();
        return TaskDO.builder()
                .taskId(idGenerator.nextTaskId())
                .taskType(TaskTypeEnum.MANUAL.name())
                .taskName(req.getStepName())
                .executionId(req.getExecutionId())
                .nodeId(req.getNodeId())
                .stepKey(req.getStepKey())
                .callbackToken(req.getCallbackToken())
                .attempt(req.getAttempt())
                .priority(req.getPriority())
                .zoneCode(req.getZoneCode())
                .status(TaskStatusEnum.PENDING.name())
                .retryCount(0)
                .deadlineAt(req.getDueHours() > 0 ? now.plusHours(req.getDueHours()) : null)
                .executorType("HUMAN")
                .queuedAt(now)
                .payload(buildPayload(req))
                .build();
    }

    private String buildPayload(ManualTaskRequest req) {
        return JSON.toJSONString(Map.of(
                "assignee",   req.getAssignee() != null ? req.getAssignee() : "",
                "formId",     req.getFormId()   != null ? req.getFormId()   : "",
                "dueHours",   req.getDueHours(),
                "waitReason", req.getWaitReason() != null ? req.getWaitReason() : "等待人工处理"
        ));
    }
}
