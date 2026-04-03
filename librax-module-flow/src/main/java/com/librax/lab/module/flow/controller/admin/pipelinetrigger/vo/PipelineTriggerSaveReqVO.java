package com.librax.lab.module.flow.controller.admin.pipelinetrigger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 流程触发配置表，管理定时和事件触发规则 [pd_]新增/修改 Request VO")
@Data
public class PipelineTriggerSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "18048")
    private Long id;

    @Schema(description = "触发器唯一业务 ID，UUID", requiredMode = Schema.RequiredMode.REQUIRED, example = "7658")
    @NotEmpty(message = "触发器唯一业务 ID，UUID不能为空")
    private String triggerId;

    @Schema(description = "关联 pd_pipeline_definition.pipeline_key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "关联 pd_pipeline_definition.pipeline_key不能为空")
    private String pipelineKey;

    @Schema(description = "指定版本，NULL 表示始终用最新 ACTIVE 版本")
    private Integer pipelineVersion;

    @Schema(description = "CRON 定时  EVENT 事件", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "CRON 定时  EVENT 事件不能为空")
    private String triggerType;

    @Schema(description = "trigger_type为CRON 时填写，如 0 0 8 * * ?")
    private String cronExpr;

    @Schema(description = "trigger_type为EVENT 时填写，MQ topic 名称")
    private String eventTopic;

    @Schema(description = "每次触发时注入的固定 input_params")
    private String fixedParams;

    @Schema(description = "触发时指定执行区域")
    private String zoneCode;

    @Schema(description = "ACTIVE 启用 | PAUSED 暂停", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "ACTIVE 启用 | PAUSED 暂停不能为空")
    private String status;

}