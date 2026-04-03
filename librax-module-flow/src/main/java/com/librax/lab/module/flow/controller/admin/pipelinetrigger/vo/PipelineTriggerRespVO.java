package com.librax.lab.module.flow.controller.admin.pipelinetrigger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 流程触发配置表，管理定时和事件触发规则 [pd_] Response VO")
@Data
@ExcelIgnoreUnannotated
public class PipelineTriggerRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "18048")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "触发器唯一业务 ID，UUID", requiredMode = Schema.RequiredMode.REQUIRED, example = "7658")
    @ExcelProperty("触发器唯一业务 ID，UUID")
    private String triggerId;

    @Schema(description = "关联 pd_pipeline_definition.pipeline_key", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("关联 pd_pipeline_definition.pipeline_key")
    private String pipelineKey;

    @Schema(description = "指定版本，NULL 表示始终用最新 ACTIVE 版本")
    @ExcelProperty("指定版本，NULL 表示始终用最新 ACTIVE 版本")
    private Integer pipelineVersion;

    @Schema(description = "CRON 定时  EVENT 事件", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("CRON 定时  EVENT 事件")
    private String triggerType;

    @Schema(description = "trigger_type为CRON 时填写，如 0 0 8 * * ?")
    @ExcelProperty("trigger_type为CRON 时填写，如 0 0 8 * * ?")
    private String cronExpr;

    @Schema(description = "trigger_type为EVENT 时填写，MQ topic 名称")
    @ExcelProperty("trigger_type为EVENT 时填写，MQ topic 名称")
    private String eventTopic;

    @Schema(description = "每次触发时注入的固定 input_params")
    @ExcelProperty("每次触发时注入的固定 input_params")
    private String fixedParams;

    @Schema(description = "触发时指定执行区域")
    @ExcelProperty("触发时指定执行区域")
    private String zoneCode;

    @Schema(description = "ACTIVE 启用 | PAUSED 暂停", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("ACTIVE 启用 | PAUSED 暂停")
    private String status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}