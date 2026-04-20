package com.librax.lab.module.task.enums;

import com.librax.lab.framework.common.exception.ErrorCode;

/**
 * Resource 错误码枚举类
 * <p>
 * flow 系统，使用 1_060_001_000 段
 */
public interface ErrorCodeConstants {


    // ── 任务定义 ──────────────────────────────────────────────────────
    ErrorCode TASK_NOT_EXISTS =
            new ErrorCode(1_060_001_000, "任务不存在");

    ErrorCode EVENT_NOT_EXISTS =
            new ErrorCode(1_060_001_001, "任务事件不存在");

    ErrorCode EXECUTOR_CONFIG_NOT_EXISTS =
            new ErrorCode(1_060_001_002, "任务配置不存在");



}

