
package com.librax.lab.module.flow.engine.execution.exception;

import lombok.Builder;
import lombok.Data;

/**
 * 重试策略
 *
 * 定义某种错误类型是否可重试、重试次数、退避时间等
 */
@Data
@Builder
public class RetryPolicy {

    /** 错误码模式（支持前缀匹配，如 DEVICE_ 匹配所有设备错误） */
    private String errorCodePattern;

    /** 是否可重试 */
    private boolean retryable;

    /** 最大重试次数（覆盖步骤级配置，null 表示用步骤级的 maxAttempts） */
    private Integer maxRetries;

    /** 退避时间（ms），null 表示用步骤级的 backoffMs */
    private Long backoffMs;

    /** 是否需要告警 */
    private boolean alertOnFail;

    /** 告警级别：INFO / WARNING / CRITICAL */
    private String alertLevel;

    /** 描述（给人看的） */
    private String description;
}
