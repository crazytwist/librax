package com.librax.lab.module.device.api.enums;

import lombok.Getter;

/**
 * 设备健康状态
 */
@Getter
public enum DeviceHealthStatusEnum {

    /** 健康,可正常调度 */
    HEALTHY("健康"),

    /** 降级(部分功能受限,但仍可用) */
    DEGRADED("降级"),

    /** 故障,不可调度 */
    FAULT("故障"),

    /** 未知(心跳超时或首次接入) */
    UNKNOWN("未知");

    private final String desc;

    DeviceHealthStatusEnum(String desc) {
        this.desc = desc;
    }

    /** 是否可被调度使用 */
    public boolean isUsable() {
        return this == HEALTHY || this == DEGRADED;
    }
}