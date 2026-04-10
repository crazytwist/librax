package com.librax.lab.module.device.controller.admin.devicecommand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_] Response VO")
@Data
@ExcelIgnoreUnannotated
public class DeviceCommandRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "27547")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "设备类型，与 lab_device_info.device_type 对应", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("设备类型，与 lab_device_info.device_type 对应")
    private String deviceType;

    @Schema(description = "指令代码，如 MEASURE / COLLECT / CALIBRATE", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("指令代码，如 MEASURE / COLLECT / CALIBRATE")
    private String commandCode;

    @Schema(description = "请求报文模板，支持 ${param} 占位符替换", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("请求报文模板，支持 ${param} 占位符替换")
    private String requestTemplate;

    @Schema(description = "HTTP Content-Type，TCP/SERIAL时为报文编码格式：HEX/ASCII/BINARY", example = "1")
    @ExcelProperty("HTTP Content-Type，TCP/SERIAL时为报文编码格式：HEX/ASCII/BINARY")
    private String contentType;

    @Schema(description = "HTTP方法：GET/POST/PUT，仅HTTP协议有效")
    @ExcelProperty("HTTP方法：GET/POST/PUT，仅HTTP协议有效")
    private String httpMethod;

    @Schema(description = "HTTP请求路径，如 /api/measure，拼接到 base_url 后")
    @ExcelProperty("HTTP请求路径，如 /api/measure，拼接到 base_url 后")
    private String httpPath;

    @Schema(description = "指令执行超时(ms)，覆盖设备默认值")
    @ExcelProperty("指令执行超时(ms)，覆盖设备默认值")
    private Long timeoutMs;

    @Schema(description = "是否可重试")
    @ExcelProperty("是否可重试")
    private Boolean retryable;

    @Schema(description = "关联 lab_device_codec.id，NULL时原样返回响应体", example = "12902")
    @ExcelProperty("关联 lab_device_codec.id，NULL时原样返回响应体")
    private Long codecId;

    @Schema(description = "轮询结果的HTTP路径，如 /api/result/${taskId}")
    @ExcelProperty("轮询结果的HTTP路径，如 /api/result/${taskId}")
    private String pollPath;

    @Schema(description = "判断完成的表达式")
    @ExcelProperty("判断完成的表达式")
    private String pollDoneExpr;

    @Schema(description = "最大轮询次数")
    @ExcelProperty("最大轮询次数")
    private Integer pollMaxTimes;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}