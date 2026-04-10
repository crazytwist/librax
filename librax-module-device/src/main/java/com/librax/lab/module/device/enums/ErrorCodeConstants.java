package com.librax.lab.module.device.enums;

import com.librax.lab.framework.common.exception.ErrorCode;

/**
 * Resource 错误码枚举类
 * <p>
 * lab 系统，使用 1_050_002_000 段
 */
public interface ErrorCodeConstants {


    // ── 设备信息 ──────────────────────────────────────────────────────
    ErrorCode DEVICE_INFO_NOT_EXISTS =
            new ErrorCode(1_050_002_000, "设备定义不存在");

    ErrorCode DEVICE_COMMAND_NOT_EXISTS =
            new ErrorCode(1_050_002_001, "设备命令不存在");

    ErrorCode DEVICE_CODEC_NOT_EXISTS =
            new ErrorCode(1_050_002_003, "设备解析规则不存在");


}

