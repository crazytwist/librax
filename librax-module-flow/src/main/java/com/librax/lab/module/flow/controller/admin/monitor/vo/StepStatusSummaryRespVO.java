package com.librax.lab.module.flow.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 步骤状态汇总")
@Data
@Builder
public class StepStatusSummaryRespVO {

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "步骤标识")
    private String stepKey;

    @Schema(description = "步骤类型")
    private String stepType;

    @Schema(description = "当前状态")
    private String status;

    @Schema(description = "当前尝试次数")
    private Integer attempt;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    private LocalDateTime finishedAt;

    @Schema(description = "执行耗时(ms)")
    private long executeMs;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMsg;
}
