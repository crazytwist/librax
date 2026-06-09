package com.librax.lab.module.flow.api.task;

import lombok.Builder;
import lombok.Data;

/**
 * 人工任务创建请求
 * <p>
 * {@link ManualTaskSpi#submitTask} 的入参，由 ManualStepExecutor 组装，
 * task 模块据此创建 lab_task 记录并推入任务队列。
 */
@Data
@Builder
public class ManualTaskRequest {

    /** 流程执行ID */
    private String executionId;

    /** 步骤节点ID */
    private String nodeId;

    /** 步骤标识（如 manual_confirm） */
    private String stepKey;

    /** 步骤显示名称，作为待办标题 */
    private String stepName;

    /** 回调令牌，人工完成后携带此 token 通知引擎 */
    private String callbackToken;

    /** 第几次尝试 */
    private int attempt;

    /** 指定操作人（登录名/工号），为空则由业务侧自行分配 */
    private String assignee;

    /** 表单ID，操作人需要填写的表单模板，为空则只需确认 */
    private String formId;

    /** 截止时间（小时），默认 24h */
    @Builder.Default
    private int dueHours = 24;

    /** 等待原因，展示给操作人的说明文字 */
    private String waitReason;

    /** 优先级：0普通 1加急 2特急 */
    private int priority;

    /** 执行区域 */
    private String zoneCode;
}
