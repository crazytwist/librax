package com.librax.lab.module.device.controller.admin.devicecommand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 管理后台 - 设备指令直接执行 Response VO
 *
 * <p>execute 接口立即返回此对象，status=EXECUTING 时可通过 execId 轮询 getResult 接口获取最终结果。
 */
@Schema(description = "管理后台 - 设备指令直接执行 Response VO")
@Data
public class DeviceCommandExecuteRespVO {

    @Schema(description = "执行ID，用于轮询结果（格式：de-{uuid}）", example = "de-a1b2c3d4e5f6")
    private String execId;

    @Schema(description = "执行状态：EXECUTING（执行中）/ SUCCESS（成功）/ FAILED（失败）/ TIMEOUT（超时）/ NOT_FOUND（记录不存在或已过期）")
    private String status;

    @Schema(description = "实际执行的设备ID")
    private String deviceId;

    @Schema(description = "设备侧任务ID（由设备驱动返回）")
    private String taskId;

    @Schema(description = "回调令牌，设备回调时须原样携带，用于验证合法性。" +
            "回调地址：POST /app-api/device/callback/{execId}/direct",
            example = "a3f9b2c1d4e5f6a7b8c9d0e1f2a3b4c5")
    private String callbackToken;

    @Schema(description = "执行输出数据，status=SUCCESS 时有值，结构由指令 outputFields 定义")
    private Map<String, Object> output;

    @Schema(description = "设备原始响应报文（调试用，仅设备返回了原始报文时有值）")
    private String rawResponse;

    @Schema(description = "错误码，status=FAILED 时有值")
    private String errorCode;

    @Schema(description = "错误信息，status=FAILED 时有值")
    private String errorMsg;

    @Schema(description = "实际执行耗时(ms)，从发送到收到回调的时间")
    private Long executeMs;

}
