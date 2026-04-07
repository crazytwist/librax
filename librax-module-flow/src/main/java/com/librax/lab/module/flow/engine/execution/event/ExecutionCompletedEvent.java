package com.librax.lab.module.flow.engine.execution.event;

import lombok.Getter;

/**
 * 流程完成事件
 */
@Getter
public class ExecutionCompletedEvent extends BaseFlowEvent {
    private final boolean success;

    public ExecutionCompletedEvent(Object source, String executionId,
                                   boolean success) {
        super(source, executionId);
        this.success = success;
    }
}
