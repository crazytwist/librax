package com.librax.lab.module.task.enums;

import lombok.Getter;

@Getter
public enum TaskTypeEnum {

    INSTRUMENT("仪器检测"),
    AGV("AGV搬运"),
    COMPUTE("计算任务"),
    MANUAL("人工任务"),
    NOTIFY("通知任务");

    private final String desc;

    TaskTypeEnum(String desc) { this.desc = desc; }
}

