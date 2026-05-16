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
     * 当前流程执行 ID
     * 用于 ResourcePool 查父执行链，实现资源继承
     * 可为 null（系统级任务不走继承逻辑）
     */
    private String executionId;

    /**
     * 当前节点 ID
     * 用于写 INHERITED 类型的 hold 记录
     */
    private String nodeId;

    /**
     * 当前重试次数
     * 用于写 INHERITED 类型的 hold 记录
     */
    private int attempt;

    /**
     * 资源类型（必填）
     * 和 lab_resource_config.resource_type 对应，如 PH_METER / AGV
     */
    private String resourceType;

    /**
     * 期望区域
     * 独占资源：必须匹配 zone_code，否则申请失败
     * 共享资源：用于区域配额计数
     * 传 null 表示不限区域（仅适用于共享资源）
     */
    private String zoneCode;

    /**
     * 持有者标识（必填）
     * 典型格式：{executionId}:{nodeId}:{attempt}
     * 释放时必须用同一个标识，防止误释放
     */
    private String holderKey;

    /**
     * 持有超时(ms)
     * 对应 Redis 锁的 TTL = holdTimeoutMs + 30s 缓冲
     * 到期自动释放，防死锁
     */
    private long holdTimeoutMs;

    /**
     * 是否允许跨区借用共享资源（软隔离降级）
     * true：本区独占资源不足时，允许申请共享池资源
     * false：严格本区独占，拿不到就失败
     */
    @Builder.Default
    private boolean allowSharedFallback = true;
}