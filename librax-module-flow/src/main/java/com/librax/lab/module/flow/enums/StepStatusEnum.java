package com.librax.lab.module.flow.enums;


public enum StepStatusEnum {

    PENDING,        // 等待前置节点完成
    RUNNING,        // 执行中
    SUCCESS,        // 执行成功
    FAILED,         // 本次尝试失败（attempt < maxAttempts，还可重试）
    SKIPPED,        // 被跳过（CONDITION 未选中的分支）
    DEAD,           // 彻底失败（attempt >= maxAttempts，不再重试）
    COMPENSATING,   // 补偿中
    COMPENSATED,    // 补偿完成
    WAITING;        // 等待外部信号（设备回调/人工审批/外部事件）

    public boolean isTerminal() {
        return this == SUCCESS || this == SKIPPED
                || this == DEAD || this == COMPENSATED;
    }

    /**
     * 调度器判断前置依赖是否满足时使用，SKIPPED 等同于 SUCCESS
     */
    public boolean isDependencySatisfied() {
        return this == SUCCESS || this == SKIPPED;
    }
}
