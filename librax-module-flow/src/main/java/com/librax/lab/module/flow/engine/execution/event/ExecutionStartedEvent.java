package com.librax.lab.module.flow.engine.execution.event;

import lombok.Getter;

/**
 * 流程启动事件
 */
@Getter
public class ExecutionStartedEvent extends BaseFlowEvent {
    private final String pipelineKey;
    private final int pipelineVersion;

    public ExecutionStartedEvent(Object source, String executionId,
                                 String pipelineKey, int pipelineVersion) {
        super(source, executionId);
        this.pipelineKey = pipelineKey;
        this.pipelineVersion = pipelineVersion;
    }
}