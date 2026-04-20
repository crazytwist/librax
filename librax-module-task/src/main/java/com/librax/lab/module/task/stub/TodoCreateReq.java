package com.librax.lab.module.task.stub;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TodoCreateReq {

    /**
     * 待办标题
     */
    private String title;

    /**
     * 负责人，用户ID或角色编码
     */
    private String assignee;

    /**
     * 表单ID，前端渲染操作表单用
     */
    private String formId;

    /**
     * 回调令牌
     * 操作人在前端确认后，接口必须携带此 token 调用
     * POST /app-api/task/manual/callback?callbackToken=xxx
     */
    private String callbackToken;

    /**
     * 截止时间
     */
    private LocalDateTime dueAt;
}