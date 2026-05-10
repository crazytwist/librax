package com.librax.lab.module.device.controller.vo;


import lombok.Data;
import java.util.Map;

@Data
public class DeviceCallbackReqVO {

    /** 回调令牌，创建等待时生成，必须携带 */
    private String callbackToken;

    /** 是否成功 */
    private boolean success;

    /** 成功时的输出数据，如 {"ph": 7.2, "temperature": 25.1} */
    private Map<String, Object> data;

    /** 失败时的错误码 */
    private String errorCode;

    /** 失败时的错误信息 */
    private String errorMsg;

    // ── 以下为可选字段，设备发送原始报文时使用 ──

    /** 设备类型（用于查找 codec 配置） */
    private String deviceType;

    /** 指令编码（用于查找 codec 配置） */
    private String commandCode;

    /** 设备原始响应报文（需要经过 CodecExecutor 解析） */
    private String rawResponse;
}