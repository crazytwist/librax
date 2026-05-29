package com.librax.lab.module.lab.enums;

import com.librax.lab.framework.common.exception.ErrorCode;

/**
 * Lab 错误码枚举类
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


    // ── 物料信息 ──────────────────────────────────────────────────────
    ErrorCode CONTAINER_TYPE_NOT_EXISTS =
            new ErrorCode(1_050_001_005, "容器类型定义不存在");

    ErrorCode MATERIAL_INSTANCE_NOT_EXISTS =
            new ErrorCode(1_050_001_006, "物料实例不存在");

    ErrorCode MATERIAL_DEF_NOT_EXISTS =
            new ErrorCode(1_050_001_007, "内容物定义不存在");

    ErrorCode MATERIAL_CHECK_RULE_NOT_EXISTS =
            new ErrorCode(1_050_001_008, "步骤物料前置检查规则不存在");

    ErrorCode MATERIAL_CONSUMPTION_NOT_EXISTS =
            new ErrorCode(1_050_001_009, "步骤物料消耗记录不存在");


}

