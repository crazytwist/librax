package com.librax.lab.module.resource.enums;

import com.librax.lab.framework.common.exception.ErrorCode;

/**
 * Resource 错误码枚举类
 * <p>
 * flow 系统，使用 1_070_001_000 段
 */
public interface ErrorCodeConstants {

    // ── 资源定义 ──────────────────────────────────────────────────────
    ErrorCode ZONE_QUOTA_NOT_EXISTS =
            new ErrorCode(1_070_001_000, "区域对共享资源的配额不存在");

    ErrorCode CONFIG_NOT_EXISTS =
            new ErrorCode(1_070_001_001, "资源配置表不存在");

    ErrorCode STEP_RESOURCE_HOLD_NOT_EXISTS =
            new ErrorCode(1_070_001_002, "步骤资源持有不存在");

    ErrorCode STEP_RESOURCE_REQ_NOT_EXISTS =
            new ErrorCode(1_070_001_003, "步骤资源需求定义不存在");

    ErrorCode RACK_INFO_NOT_EXISTS =
            new ErrorCode(1_070_001_004, "库位的上级容器定义不存在");

    ErrorCode SLOT_INFO_NOT_EXISTS =
            new ErrorCode(1_070_001_005, "库位资源定义不存在");


}
