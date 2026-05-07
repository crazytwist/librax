package com.librax.lab.module.flow.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 执行时间线事件")
@Data
@Builder
public class ExecutionTimelineRespVO {

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "节点ID（流程级事件为空）")
    private String nodeId;

    @Schema(description = "尝试次数")
    private Integer attempt;

    @Schema(description = "变更前状态")
    private String fromStatus;

    @Schema(description = "变更后状态")
    private String toStatus;

    @Schema(description = "事件附加数据")
    private String payload;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "发生时间")
    private LocalDateTime occurredAt;
}
