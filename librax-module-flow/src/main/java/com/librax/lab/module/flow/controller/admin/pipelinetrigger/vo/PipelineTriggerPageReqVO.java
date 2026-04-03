package com.librax.lab.module.flow.controller.admin.pipelinetrigger.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 流程触发配置表，管理定时和事件触发规则 [pd_]分页 Request VO")
@Data
public class PipelineTriggerPageReqVO extends PageParam {

    @Schema(description = "触发器唯一业务 ID，UUID", example = "7658")
    private String triggerId;

    @Schema(description = "关联 pd_pipeline_definition.pipeline_key")
    private String pipelineKey;

    @Schema(description = "CRON 定时  EVENT 事件", example = "1")
    private String triggerType;

    @Schema(description = "ACTIVE 启用 | PAUSED 暂停", example = "1")
    private String status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}