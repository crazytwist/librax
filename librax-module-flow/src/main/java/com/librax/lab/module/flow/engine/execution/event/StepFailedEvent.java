package com.librax.lab.module.flow.engine.execution.event;

import lombok.Getter;

/**
 * 步骤失败事件
 */
@Getter
public class StepFailedEvent extends BaseFlowEvent {
    private final String nodeId;
    private final int attempt;
    private final String stepType;
    private final String errorCode;
    private final String errorMsg;

    public StepFailedEvent(Object source, String executionId,
                           String nodeId, int attempt, String stepType,
                           String errorCode, String errorMsg) {
        super(source, executionId);
        this.nodeId = nodeId;
        this.attempt = attempt;
        this.stepType = stepType;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}
