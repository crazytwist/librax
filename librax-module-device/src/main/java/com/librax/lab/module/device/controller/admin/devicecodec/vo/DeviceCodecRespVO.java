package com.librax.lab.module.device.controller.admin.devicecodec.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_] Response VO")
@Data
@ExcelIgnoreUnannotated
public class DeviceCodecRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "26450")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "规则名称，便于识别，如 雷磁PH计-测量结果解析", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @ExcelProperty("规则名称，便于识别，如 雷磁PH计-测量结果解析")
    private String codecName;

    @Schema(description = "解析类型：JSON / HEX / REGEX / SCRIPT", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("解析类型：JSON / HEX / REGEX / SCRIPT")
    private String parseType;

    @Schema(description = "JSONPath 字段映射，key=输出字段名 value=JSONPath表达式")
    @ExcelProperty("JSONPath 字段映射，key=输出字段名 value=JSONPath表达式")
    private String fieldMapping;

    @Schema(description = "十六进制按位解析规则")
    @ExcelProperty("十六进制按位解析规则")
    private String hexRules;

    @Schema(description = "正则提取规则")
    @ExcelProperty("正则提取规则")
    private String regexRules;

    @Schema(description = "脚本引擎：groovy / js")
    @ExcelProperty("脚本引擎：groovy / js")
    private String scriptEngine;

    @Schema(description = "解析脚本，入参为原始响应字符串，返回 Map")
    @ExcelProperty("解析脚本，入参为原始响应字符串，返回 Map")
    private String scriptContent;

    @Schema(description = "单位换算规则，解析后应用")
    @ExcelProperty("单位换算规则，解析后应用")
    private String unitConversions;

    @Schema(description = "有效值范围校验")
    @ExcelProperty("有效值范围校验")
    private String validRange;

    @Schema(description = "备注", example = "你说的对")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}