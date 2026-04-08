
package com.librax.lab.module.flow.engine.execution.exception;

import lombok.Builder;
import lombok.Data;

/**
 * 失败决策结果
 *
 * <p>由 {@link ExceptionEngine#decide} 返回，告诉调用方下一步该怎么做。
 * 调用方（DagScheduler / FailureActionHelper）拿到后自己执行。
 *
 * <p>这是一个纯值对象，不持有任何 Spring Bean 引用，
 * 从设计上保证 ExceptionEngine 是无副作用的决策器。
 */
@Data
@Builder
public class FailureDecision {

    public enum Action {
        /** 插入重试行 + 延迟重调度 */
        RETRY,
        /** 标记 DEAD → 终止整条流程（FAIL_FAST） */
        DEAD_FAIL_FAST,
        /** 标记 DEAD → 跳过该节点，继续调度后续节点（CONTINUE_ON_FAIL） */
        DEAD_CONTINUE,
    }

    /** 决策动作 */
    private Action action;

    /** RETRY 时有效：下一次尝试次数 */
    private int nextAttempt;

    /** RETRY 时有效：退避时间(ms) */
    private long backoffMs;

    /** RETRY 时有效：插入重试行需要的 stepKey */
    private String stepKey;

    /** RETRY 时有效：插入重试行需要的 stepType */
    private String stepType;

    /** 是否需要告警 */
    private boolean alertOnFail;

    /** 告警级别：INFO / WARNING / CRITICAL */
    private String alertLevel;

    /** 策略描述（日志用） */
    private String description;
}