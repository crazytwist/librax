package com.librax.lab.module.flow.engine.execution.event;

import lombok.Getter;

/**
 * 步骤进入等待事件
 */
@Getter
public class StepWaitingEvent extends BaseFlowEvent {
    private final String nodeId;
    private final int attempt;
    private final String waitingFor;

    public StepWaitingEvent(Object source, String executionId,
                            String nodeId, int attempt, String waitingFor) {
        super(source, executionId);
        this.nodeId = nodeId;
        this.attempt = attempt;
        this.waitingFor = waitingFor;
    }
}