package com.librax.lab.module.flow.api.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源申请结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcquireResult {

    /** 是否申请成功 */
    private boolean success;

    /** 成功时:分配到的资源 ID(如 PH-METER-01) */
    private String resourceId;

    /** 失败时:原因码 */
    private AcquireFailReasonEnum reason;

    /** 失败时:建议等待多久再重试(ms),由调用方决定是否采纳 */
    private long suggestRetryMs;

    public static AcquireResult ok(String resourceId) {
        return new AcquireResult(true, resourceId, null, 0L);
    }

    public static AcquireResult fail(AcquireFailReasonEnum reason, long suggestRetryMs) {
        return new AcquireResult(false, null, reason, suggestRetryMs);
    }
}