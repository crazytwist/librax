package com.librax.lab.module.lab.controller.admin.sampleevent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]新增/修改 Request VO")
@Data
public class SampleEventSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "9889")
    private Long id;

    @Schema(description = "样本ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "14191")
    @NotEmpty(message = "样本ID不能为空")
    private String sampleId;

    @Schema(description = "关联流程执行ID，非流程操作为NULL", example = "28320")
    private String executionId;

    @Schema(description = "关联节点ID", example = "17385")
    private String nodeId;

    @Schema(description = "事件类型：REGISTERED/RECEIVED/LOADED/SPLIT/MERGE/ALIQUOT/ADD_REAGENT/TRANSFER/BOUND_TO_STEP/MEASURING/MEASURED/COMPLETED/ARCHIVED/REJECTED/LOST/STATUS_CHANGED", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "事件类型：REGISTERED/RECEIVED/LOADED/SPLIT/MERGE/ALIQUOT/ADD_REAGENT/TRANSFER/BOUND_TO_STEP/MEASURING/MEASURED/COMPLETED/ARCHIVED/REJECTED/LOST/STATUS_CHANGED不能为空")
    private String eventType;

    @Schema(description = "操作前状态", example = "2")
    private String fromStatus;

    @Schema(description = "操作后状态", example = "1")
    private String toStatus;

    @Schema(description = "操作前位置")
    private String locationFrom;

    @Schema(description = "操作后位置")
    private String locationTo;

    @Schema(description = "操作前体积")
    private BigDecimal volumeBeforeUl;

    @Schema(description = "操作后体积")
    private BigDecimal volumeAfterUl;

    @Schema(description = "关联设备ID", example = "27857")
    private String deviceId;

    @Schema(description = "操作人或设备ID")
    private String operator;

    @Schema(description = "事件发生时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "事件发生时间不能为空")
    private LocalDateTime occurredAt;

    @Schema(description = "附加信息，如拆分明细、试剂信息、检测参数等")
    private String payload;

    @Schema(description = "事件备注", example = "你说的对")
    private String remark;

}