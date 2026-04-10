package com.librax.lab.module.device.controller.admin.devicecodec.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]新增/修改 Request VO")
@Data
public class DeviceCodecSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "26450")
    private Long id;

    @Schema(description = "规则名称，便于识别，如 雷磁PH计-测量结果解析", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @NotEmpty(message = "规则名称，便于识别，如 雷磁PH计-测量结果解析不能为空")
    private String codecName;

    @Schema(description = "解析类型：JSON / HEX / REGEX / SCRIPT", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "解析类型：JSON / HEX / REGEX / SCRIPT不能为空")
    private String parseType;

    @Schema(description = "JSONPath 字段映射，key=输出字段名 value=JSONPath表达式")
    private String fieldMapping;

    @Schema(description = "十六进制按位解析规则")
    private String hexRules;

    @Schema(description = "正则提取规则")
    private String regexRules;

    @Schema(description = "脚本引擎：groovy / js")
    private String scriptEngine;

    @Schema(description = "解析脚本，入参为原始响应字符串，返回 Map")
    private String scriptContent;

    @Schema(description = "单位换算规则，解析后应用")
    private String unitConversions;

    @Schema(description = "有效值范围校验")
    private String validRange;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

}