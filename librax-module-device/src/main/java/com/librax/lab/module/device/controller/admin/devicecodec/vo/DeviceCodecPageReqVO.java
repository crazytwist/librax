package com.librax.lab.module.device.controller.admin.devicecodec.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]分页 Request VO")
@Data
public class DeviceCodecPageReqVO extends PageParam {

    @Schema(description = "规则名称，便于识别，如 雷磁PH计-测量结果解析", example = "芋艿")
    private String codecName;

    @Schema(description = "解析类型：JSON / HEX / REGEX / SCRIPT", example = "1")
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

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}