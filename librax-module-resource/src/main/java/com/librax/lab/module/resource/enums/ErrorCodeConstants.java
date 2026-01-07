package com.librax.lab.module.resource.enums;

import com.librax.lab.framework.common.exception.ErrorCode;

/**
 * Resource 错误码枚举类
 * <p>
 * resource 系统，使用 1-006-000-000 段
 */
public interface ErrorCodeConstants {

    ErrorCode LOCATION_NOT_EXISTS = new ErrorCode(1_006_000_001, "区位信息不存在");
    ErrorCode MATERIAL_CONFIG_NOT_EXISTS = new ErrorCode(1_006_000_002, "物料配置不存在");
    ErrorCode MATERIAL_NOT_EXISTS = new ErrorCode(1_006_000_003, "物料信息不存在");

}
