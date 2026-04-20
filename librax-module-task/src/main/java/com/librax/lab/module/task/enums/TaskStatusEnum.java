package com.librax.lab.module.task.enums;

import lombok.Getter;

@Getter
public enum TaskStatusEnum {

    PENDING("待执行"),
    ASSIGNED("已分配"),
    EXECUTING("执行中"),
    DONE("已完成"),
    FAILED("已失败"),
    CANCELLED("已取消"),
    TIMEOUT("已超时");

    private final String desc;

    TaskStatusEnum(String desc) { this.desc = desc; }

    public boolean isTerminal() {
        return this == DONE || this == FAILED
                || this == CANCELLED || this == TIMEOUT;
    }
}

