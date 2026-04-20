package com.librax.lab.module.task.stub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TodoServiceStub implements TodoService {

    @Override
    public String create(TodoCreateReq req) {
        String todoId = "TODO-STUB-" + System.currentTimeMillis();
        log.info("[TodoServiceStub] 创建待办（STUB） todoId={} title={} assignee={} token={}",
                todoId, req.getTitle(), req.getAssignee(), req.getCallbackToken());
        // TODO: 对接真实审批系统后删除此实现，实现 TodoService 接口替换
        return todoId;
    }

    @Override
    public void cancel(String todoId) {
        log.info("[TodoServiceStub] 取消待办（STUB） todoId={}", todoId);
        // TODO: 对接真实审批系统后实现
    }
}

