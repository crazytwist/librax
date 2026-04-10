package com.librax.lab.module.device.enums;


import lombok.Getter;

@Getter
public enum DeviceStatus {

    /** 空闲，可接受新指令 */
    IDLE("空闲"),

    /** 忙碌，正在执行任务 */
    BUSY("忙碌"),

    /** 离线，连接断开 */
    OFFLINE("离线"),

    /** 故障，设备报错 */
    FAULT("故障");

    private final String desc;

    DeviceStatus(String desc) {
        this.desc = desc;
    }

    /** 是否可以接受新指令 */
    public boolean isAvailable() {
        return this == IDLE;
    }

    /** 是否处于异常状态 */
    public boolean isAbnormal() {
        return this == OFFLINE || this == FAULT;
    }
}