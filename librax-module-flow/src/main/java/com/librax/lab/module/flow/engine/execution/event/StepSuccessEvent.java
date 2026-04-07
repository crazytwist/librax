package com.librax.lab.module.flow.engine.execution.event;

import lombok.Getter;

/**
 * 步骤成功事件
 */
@Getter
public class StepSuccessEvent extends BaseFlowEvent {
    private final String nodeId;
    private final int attempt;
    private final String stepType;

    public StepSuccessEvent(Object source, String executionId,
                            String nodeId, int attempt, String stepType) {
        super(source, executionId);
        this.nodeId = nodeId;
        this.attempt = attempt;
        this.stepType = stepType;
    }
}