package com.librax.lab.module.flow.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 运行中流程")
@Data
@Builder
public class RunningExecutionRespVO {

    @Schema(description = "执行实例ID")
    private String executionId;

    @Schema(description = "流程标识")
    private String pipelineKey;

    @Schema(description = "流程版本")
    private Integer pipelineVersion;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "触发人")
    private String triggeredBy;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "已运行时长(ms)")
    private Long elapsedMs;

    @Schema(description = "总步骤数")
    private int totalSteps;

    @Schema(description = "各状态步骤数量")
    private Map<String, Long> stepStatusCount;
}
