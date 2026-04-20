package com.librax.lab.module.flow.api.enums;

public enum WaitingForEnum {
    DEVICE_CALLBACK,   // 等设备回调
    MANUAL_APPROVE,    // 等人工审批
    EXTERNAL_EVENT,    // 等外部事件
    TIMER,             // 等定时触发
    TASK               // 等任务回调
}

