package com.librax.lab.module.flow.api.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源申请结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquireResult {

    /** 是否申请成功 */
    private boolean success;

    /** 成功时：分配到的资源 ID（如 PH-METER-01） */
    private String resourceId;

    /** 失败时：原因码 */
    private AcquireFailReasonEnum reason;

    /** 失败时：建议等待多久再重试(ms) */
    private long suggestRetryMs;

    /** 是否继承自父流程（继承时不需要重新竞争，也不能主动释放） */
    private boolean inherited;

    // ── 工厂方法 ──────────────────────────────────────────────────

    /** 自己竞争申请成功 */
    public static AcquireResult ok(String resourceId) {
        return AcquireResult.builder()
                .success(true)
                .resourceId(resourceId)
                .inherited(false)
                .build();
    }

    /** 继承自父流程资源（不走竞争，直接复用） */
    public static AcquireResult inherited(String resourceId) {
        return AcquireResult.builder()
                .success(true)
                .resourceId(resourceId)
                .inherited(true)
                .build();
    }

    /** 申请失败 */
    public static AcquireResult fail(AcquireFailReasonEnum reason, long suggestRetryMs) {
        return AcquireResult.builder()
                .success(false)
                .reason(reason)
                .suggestRetryMs(suggestRetryMs)
                .inherited(false)
                .build();
    }

    /**
     * 失败时建议重试等待时长
     * 兼容旧代码调用 getRetryAfterMs()
     */
    public long getRetryAfterMs() {
        return suggestRetryMs;
    }

    /**
     * 失败时原因码
     * 兼容旧代码调用 getFailReason()
     */
    public AcquireFailReasonEnum getFailReason() {
        return reason;
    }
}