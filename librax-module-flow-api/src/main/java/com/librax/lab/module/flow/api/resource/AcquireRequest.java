package com.librax.lab.module.flow.api.resource;

import lombok.Builder;
import lombok.Data;

/**
 * 资源申请请求
 */
@Data
@Builder
public class AcquireRequest {

    /**
     * 资源类型(必填)
     * <p>
     * 和 lab_resource_config.resource_type 对应,如 PH_METER / AGV / BENCH_A
     */
    private String resourceType;

    /**
     * 期望区域
     * <p>
     * 独占资源:必须匹配 zone_code,否则申请失败
     * 共享资源:用于区域配额计数(每区最多借 N 台)
     * 传 null 表示不限区域(仅适用于共享资源)
     */
    private String zoneCode;

    /**
     * 持有者标识(必填)
     * <p>
     * 典型格式:{executionId}:{nodeId}:{attempt}
     * 释放时必须用同一个标识,防止误释放
     */
    private String holderKey;

    /**
     * 持有超时(ms)
     * <p>
     * 对应 Redis 锁的 TTL = holdTimeoutMs + 30s 缓冲
     * 到期自动释放,防死锁
     */
    private long holdTimeoutMs;

    /**
     * 是否允许跨区借用共享资源(软隔离降级)
     * <p>
     * true:本区独占资源不足时,允许申请共享池资源(AGV 这类)
     * false:严格本区独占,拿不到就失败
     * 独占资源永不支持跨区(不管这个字段是什么)
     */
    @Builder.Default
    private boolean allowSharedFallback = true;
}