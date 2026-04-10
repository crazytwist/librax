package com.librax.lab.module.device.controller.admin.deviceinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 设备基本信息表，一行一台物理设备 [lab_device_]新增/修改 Request VO")
@Data
public class DeviceInfoSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "15357")
    private Long id;

    @Schema(description = "设备唯一业务ID，如 PH-METER-01", requiredMode = Schema.RequiredMode.REQUIRED, example = "18227")
    @NotEmpty(message = "设备唯一业务ID，如 PH-METER-01不能为空")
    private String deviceId;

    @Schema(description = "设备名称，如 雷磁PH计1号", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @NotEmpty(message = "设备名称，如 雷磁PH计1号不能为空")
    private String deviceName;

    @Schema(description = "设备类型，对应 pd_step_definition.device_type，如 PH_METER", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "设备类型，对应 pd_step_definition.device_type，如 PH_METER不能为空")
    private String deviceType;

    @Schema(description = "通信协议：TCP / HTTP / SERIAL / SDK / MOCK", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "通信协议：TCP / HTTP / SERIAL / SDK / MOCK不能为空")
    private String protocol;

    @Schema(description = "所属区域，关联 lab_zone_slot")
    private String zoneCode;

    @Schema(description = "IP地址或域名，TCP/HTTP时填写")
    private String host;

    @Schema(description = "端口号，TCP/HTTP时填写")
    private Integer port;

    @Schema(description = "HTTP设备基础URL，如 http://192.168.1.10:8080", example = "https://www.iocoder.cn")
    private String baseUrl;

    @Schema(description = "HTTP回调模式：WEBHOOK(设备主动推) / POLL(主动轮询)")
    private String callbackMode;

    @Schema(description = "轮询间隔(ms)，callback_mode=POLL时有效")
    private Long pollIntervalMs;

    @Schema(description = "认证方式：NONE / BASIC / TOKEN / HMAC", example = "2")
    private String authType;

    @Schema(description = "认证配置")
    private String authConfig;

    @Schema(description = "串口号，如 COM3 / /dev/ttyUSB0")
    private String serialPort;

    @Schema(description = "波特率，如 9600 / 115200")
    private Integer baudRate;

    @Schema(description = "数据位")
    private Integer dataBits;

    @Schema(description = "停止位")
    private Integer stopBits;

    @Schema(description = "校验位：NONE/ODD/EVEN")
    private String parity;

    @Schema(description = "SDK驱动实现类全限定名，executor=SDK时使用")
    private String sdkClass;

    @Schema(description = "SDK初始化参数，透传给驱动实现类")
    private String sdkConfig;

    @Schema(description = "连接超时(ms)")
    private Long connectTimeoutMs;

    @Schema(description = "读取超时(ms)")
    private Long readTimeoutMs;

    @Schema(description = "心跳间隔(ms)，0表示不发心跳")
    private Long heartbeatIntervalMs;

    @Schema(description = "心跳指令code，引用 lab_device_command.command_code")
    private String heartbeatCommand;

    @Schema(description = "最大并发指令数，通常为1")
    private Integer maxConcurrent;

    @Schema(description = "当前状态：ONLINE/OFFLINE/FAULT，运行时由心跳维护，此处为初始值", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "当前状态：ONLINE/OFFLINE/FAULT，运行时由心跳维护，此处为初始值不能为空")
    private String status;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

}