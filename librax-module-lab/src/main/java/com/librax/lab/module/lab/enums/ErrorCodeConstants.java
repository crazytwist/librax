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
            new ErrorCode(1_050_001_000, "样本步骤不存在");

    ErrorCode SAMPLE_EVENT_NOT_EXISTS =
            new ErrorCode(1_050_001_001, "样本事件不存在");

    ErrorCode SAMPLE_RESULT_NOT_EXISTS =
            new ErrorCode(1_050_001_000, "样本结果不存在");

}

