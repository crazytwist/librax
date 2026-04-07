package com.librax.lab.module.flow.engine.execution.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 流程事件基类
 */
@Getter
public abstract class BaseFlowEvent extends ApplicationEvent {
    private final String executionId;

    public BaseFlowEvent(Object source, String executionId) {
        super(source);
        this.executionId = executionId;
    }
}