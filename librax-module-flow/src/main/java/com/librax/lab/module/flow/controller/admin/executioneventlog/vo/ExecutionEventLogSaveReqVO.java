package com.librax.lab.module.flow.controller.admin.executioneventlog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 执行事件日志，只 INSERT 不修改，全链路追踪与审计新增/修改 Request VO")
@Data
public class ExecutionEventLogSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "21793")
    private Long id;

    @Schema(description = "关联的执行实例", requiredMode = Schema.RequiredMode.REQUIRED, example = "3948")
    @NotEmpty(message = "关联的执行实例不能为空")
    private String executionId;

    @Schema(description = "节点 ID，流程级事件为 NULL", example = "1518")
    private String nodeId;

    @Schema(description = "第几次尝试，节点事件时填写")
    private Integer attempt;

    @Schema(description = "执行模式，与 pe_step_execution.run_mode 对应")
    private String runMode;

    @Schema(description = "PIPELINE_STARTED | PIPELINE_PAUSED | PIPELINE_RESUMED | PIPELINE_SUCCESS | PIPELINE_FAILED | PIPELINE_CANCELLED | STEP_QUEUED | STEP_STARTED | STEP_SUCCESS | STEP_FAILED | STEP_SKIPPED | STEP_DEAD | STEP_RETRY_SCHEDULED | STEP_COMPENSATE_TRIGGERED | STEP_COMPENSATED | STANDALONE_STARTED | STANDALONE_FINISHED", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "PIPELINE_STARTED | PIPELINE_PAUSED | PIPELINE_RESUMED | PIPELINE_SUCCESS | PIPELINE_FAILED | PIPELINE_CANCELLED | STEP_QUEUED | STEP_STARTED | STEP_SUCCESS | STEP_FAILED | STEP_SKIPPED | STEP_DEAD | STEP_RETRY_SCHEDULED | STEP_COMPENSATE_TRIGGERED | STEP_COMPENSATED | STANDALONE_STARTED | STANDALONE_FINISHED不能为空")
    private String eventType;

    @Schema(description = "变更前状态", example = "1")
    private String fromStatus;

    @Schema(description = "变更后状态", example = "2")
    private String toStatus;

    @Schema(description = "事件附加数据，如错误原因、重试间隔、设备 ID、补偿执行 ID")
    private String payload;

    @Schema(description = "操作人，系统触发记 SYSTEM")
    private String operator;

    @Schema(description = "事件发生时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "事件发生时间不能为空")
    private LocalDateTime occurredAt;

}