package com.librax.lab.module.flow.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 任务完成事件
 * <p>
 * task 模块在 {@code TaskCallbackDispatcher} 里发布,flow 模块的
 * {@code TaskCompletedEventListener} 监听后推进 DAG 调度。
 * <p>
 * 放在 flow-api,task 和 flow 两个模块都能引用,打破循环依赖。
 */
@Getter
public class TaskCompletedEvent extends ApplicationEvent {

    /** 回调令牌,和 pe_step_execution.callback_token 一致 */
    private final String callbackToken;

    /** 流程执行实例 ID */
    private final String executionId;

    /** 节点 ID */
    private final String nodeId;

    /** 执行是否成功 */
    private final boolean success;

    /** 任务产出(成功时写入流程上下文) */
    private final Map<String, Object> outputs;

    /** 错误码(失败时) */
    private final String errorCode;

    /** 错误信息(失败时) */
    private final String errorMsg;

    public TaskCompletedEvent(Object source,
                              String callbackToken,
                              String executionId,
                              String nodeId,
                              boolean success,
                              Map<String, Object> outputs,
                              String errorCode,
                              String errorMsg) {
        super(source);
        this.callbackToken = callbackToken;
        this.executionId   = executionId;
        this.nodeId        = nodeId;
        this.success       = success;
        this.outputs       = outputs;
        this.errorCode     = errorCode;
        this.errorMsg      = errorMsg;
    }
}