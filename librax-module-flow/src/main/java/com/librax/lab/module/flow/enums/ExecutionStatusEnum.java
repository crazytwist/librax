package com.librax.lab.module.flow.enums;


public enum ExecutionStatusEnum {

    PENDING,        // 已创建，等待调度
    RUNNING,        // 执行中
    PAUSED,         // 已暂停，等待人工恢复
    SUCCESS,        // 执行成功
    FAILED,         // 执行失败
    CANCELLED,      // 已取消
    COMPENSATING,   // 补偿执行中
    COMPENSATED;    // 补偿完成

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED
                || this == CANCELLED || this == COMPENSATED;
    }

    public boolean canTransitionTo(ExecutionStatusEnum target) {
        return switch (this) {
            case PENDING      -> target == RUNNING || target == CANCELLED;
            case RUNNING      -> target == PAUSED  || target == SUCCESS
                    || target == FAILED   || target == CANCELLED
                    || target == COMPENSATING;
            case PAUSED       -> target == RUNNING  || target == CANCELLED;
            case FAILED       -> target == COMPENSATING || target == RUNNING; // RUNNING：手动重跑
            case COMPENSATING -> target == COMPENSATED || target == FAILED;
            default           -> false; // 终态不可再流转
        };
    }
}
