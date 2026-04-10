package com.librax.lab.module.lab.enums;

import com.librax.lab.framework.common.exception.ErrorCode;

/**
 * Resource 错误码枚举类
 * <p>
 * lab 系统，使用 1_050_001_000 段
 */
public interface ErrorCodeConstants {

    // ── 样本信息 ──────────────────────────────────────────────────────
    ErrorCode SAMPLE_INFO_NOT_EXISTS =
            new ErrorCode(1_050_001_000, "样本定义不存在");

    ErrorCode SAMPLE_RELATION_NOT_EXISTS =
            new ErrorCode(1_050_001_001, "样本关联不存在");

    ErrorCode SAMPLE_STEP_NOT_EXISTS =
            new ErrorCode(1_050_001_002, "样本步骤不存在");

    ErrorCode SAMPLE_EVENT_NOT_EXISTS =
            new ErrorCode(1_050_001_003, "样本事件不存在");

    ErrorCode SAMPLE_RESULT_NOT_EXISTS =
            new ErrorCode(1_050_001_004, "样本结果不存在");


    // ── 设备信息 ──────────────────────────────────────────────────────
    ErrorCode DEVICE_INFO_NOT_EXISTS =
            new ErrorCode(1_050_002_000, "设备定义不存在");

    ErrorCode DEVICE_COMMAND_NOT_EXISTS =
            new ErrorCode(1_050_002_001, "设备命令不存在");

    ErrorCode DEVICE_CODEC_NOT_EXISTS =
            new ErrorCode(1_050_002_003, "设备解析规则不存在");


}

