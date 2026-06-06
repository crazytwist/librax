package com.librax.lab.module.flow.api.resource;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 资源释放事件
 *
 * <p>由 {@code ResourcePoolImpl.release()} 在成功释放资源后发布。
 * flow 模块监听此事件，唤醒所有正在等待该资源类型的 PENDING 步骤重新调度。
 */
@Getter
public class ResourceReleasedEvent extends ApplicationEvent {

    private final String resourceId;
    private final String resourceType;
    /** 分区标识，无分区时为 null */
    private final String zoneCode;

    public ResourceReleasedEvent(Object source,
                                 String resourceId,
                                 String resourceType,
                                 String zoneCode) {
        super(source);
        this.resourceId   = resourceId;
        this.resourceType = resourceType;
        this.zoneCode     = zoneCode;
    }
}
