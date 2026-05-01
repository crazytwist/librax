package com.librax.lab.module.flow.api.resource;

import lombok.Getter;

/**
 * 资源申请失败原因
 */
@Getter
public enum AcquireFailReasonEnum {

    /** 类型下没有配置任何资源 */
    NO_CONFIG("资源未配置"),

    /** 所有候选资源都被占用 */
    NO_AVAILABLE("资源全部占用中"),

    /** 本区独占资源被占用且不允许降级 */
    ZONE_EXCLUSIVE_BUSY("本区独占资源繁忙"),

    /** 区域配额已满(共享资源) */
    QUOTA_EXCEEDED("区域配额已满"),

    /** 候选设备全都不健康(离线/故障) */
    NO_HEALTHY("无健康设备可用");

    private final String desc;

    AcquireFailReasonEnum(String desc) {
        this.desc = desc;
    }
}