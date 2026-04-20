package com.librax.lab.module.task.executor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import com.librax.lab.module.task.stub.TodoCreateReq;
import com.librax.lab.module.task.stub.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManualTaskExecutor implements TaskExecutor {

    private final TodoService todoService;
    private final TaskMapper taskMapper;

    @Override
    public String supportType() {
        return TaskTypeEnum.MANUAL.name();
    }

    @Override
    public void execute(TaskDO task) {
        Map<String, Object> payload = parsePayload(task);
        String assignee = (String) payload.get("assignee");
        String formId   = (String) payload.get("formId");
        int dueHours    = payload.containsKey("dueHours")
                ? ((Number) payload.get("dueHours")).intValue() : 24;

        String todoId = todoService.create(TodoCreateReq.builder()
                .title("待处理：" + task.getTaskName())
                .assignee(assignee)
                .formId(formId)
                .callbackToken(task.getCallbackToken())
                .dueAt(LocalDateTime.now().plusHours(dueHours))
                .build());

        taskMapper.updateExecuting(task.getTaskId(), todoId);

        log.info("[ManualTaskExecutor] 待办已创建 taskId={} todoId={} assignee={}",
                task.getTaskId(), todoId, assignee);
        // 不主动回调，等操作人前端确认后携带 callbackToken 调接口
    }

    @Override
    public void cancel(TaskDO task) {
        if (task.getExternalTaskId() != null) {
            try {
                todoService.cancel(task.getExternalTaskId());
            } catch (Exception e) {
                log.warn("[ManualTaskExecutor] 取消待办失败（忽略） taskId={}", task.getTaskId());
            }
        }
    }

    private Map<String, Object> parsePayload(TaskDO task) {
        return JSON.parseObject(task.getPayload(),
                new TypeReference<Map<String, Object>>() {});
    }
}