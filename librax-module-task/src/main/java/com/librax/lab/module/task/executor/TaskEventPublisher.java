package com.librax.lab.module.task.executor;


import com.alibaba.fastjson.JSON;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.dataobject.taskevent.TaskEventDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.dal.mysql.taskevent.TaskEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventPublisher {

    private final TaskMapper taskMapper;        // 查 taskType / executorId 冗余字段
    private final TaskEventMapper taskEventMapper;

    /**
     * 发布任务事件（异步写 lab_task_event）
     *
     * @param taskId     任务ID
     * @param eventType  事件类型：CREATED/ASSIGNED/STARTED/DONE/FAILED/RETRYING/CANCELLED/TIMEOUT
     * @param fromStatus 变更前状态
     * @param toStatus   变更后状态
     * @param payload    附加信息，如错误码、重试次数等
     */
    @Async("labEventListenerExecutor")
    public void publish(String taskId,
                        String eventType,
                        String fromStatus,
                        String toStatus,
                        Map<String, Object> payload) {
        try {
            // 查任务取 taskType 和 executorId（冗余字段，表结构要求必填）
            TaskDO task = taskMapper.selectByTaskId(taskId);

            LocalDateTime now = LocalDateTime.now();
            TaskEventDO event = new TaskEventDO();
            event.setTaskId(taskId);
            // taskType 冗余存，便于按类型查日志
            event.setTaskType(task != null ? task.getTaskType() : "UNKNOWN");
            event.setEventType(eventType);
            event.setFromStatus(fromStatus);
            event.setToStatus(toStatus);
            // executorId：已分配时记录执行单元，未分配时为 null
            event.setExecutorId(task != null ? task.getExecutorId() : null);
            event.setPayload(payload != null ? JSON.toJSONString(payload) : null);
            event.setOperator("SYSTEM");
            event.setOccurredAt(now);
            event.setCreator("SYSTEM");
            event.setUpdater("SYSTEM");
            event.setCreateTime(now);
            event.setUpdateTime(now);
            event.setDeleted(false);
            taskEventMapper.insert(event);

        } catch (Exception e) {
            log.error("[TaskEventPublisher] 事件写入失败 taskId={} eventType={}",
                    taskId, eventType, e);
        }
    }
}