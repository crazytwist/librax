package com.librax.lab.module.flow.engine.execution.event;

import lombok.Getter;

/**
 * 步骤开始事件
 */
@Getter
public class StepStartedEvent extends BaseFlowEvent {
    private final String nodeId;
    private final int attempt;
    private final String stepType;

    public StepStartedEvent(Object source, String executionId,
                            String nodeId, int attempt, String stepType) {
        super(source, executionId);
        this.nodeId = nodeId;
        this.attempt = attempt;
        this.stepType = stepType;
    }
}