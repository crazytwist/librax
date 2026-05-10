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

    // ── 设备运行时 ──────────────────────────────────────────────────────
    ErrorCode DEVICE_NO_AVAILABLE =
            new ErrorCode(1_050_002_010, "无可用设备");
    ErrorCode DEVICE_SEND_FAILED =
            new ErrorCode(1_050_002_011, "设备指令发送失败");
    ErrorCode DEVICE_CALLBACK_INVALID =
            new ErrorCode(1_050_002_012, "设备回调参数无效");
    ErrorCode DEVICE_CODEC_PARSE_FAILED =
            new ErrorCode(1_050_002_013, "设备响应解析失败");
    ErrorCode DEVICE_POLL_TIMEOUT =
            new ErrorCode(1_050_002_014, "设备轮询超时");
    ErrorCode DEVICE_HEARTBEAT_FAILED =
            new ErrorCode(1_050_002_015, "设备心跳失败");
    ErrorCode DEVICE_PROTOCOL_UNSUPPORTED =
            new ErrorCode(1_050_002_016, "不支持的设备协议");
    ErrorCode DEVICE_AUTH_FAILED =
            new ErrorCode(1_050_002_017, "设备认证失败");
    ErrorCode DEVICE_TCP_CONNECT_FAILED =
            new ErrorCode(1_050_002_018, "TCP连接失败");

}

