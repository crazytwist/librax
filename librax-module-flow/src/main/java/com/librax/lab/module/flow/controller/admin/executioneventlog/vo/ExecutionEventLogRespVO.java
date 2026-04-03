package com.librax.lab.module.flow.controller.admin.executioneventlog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 执行事件日志，只 INSERT 不修改，全链路追踪与审计 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ExecutionEventLogRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "21793")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "关联的执行实例", requiredMode = Schema.RequiredMode.REQUIRED, example = "3948")
    @ExcelProperty("关联的执行实例")
    private String executionId;

    @Schema(description = "节点 ID，流程级事件为 NULL", example = "1518")
    @ExcelProperty("节点 ID，流程级事件为 NULL")
    private String nodeId;

    @Schema(description = "第几次尝试，节点事件时填写")
    @ExcelProperty("第几次尝试，节点事件时填写")
    private Integer attempt;

    @Schema(description = "执行模式，与 pe_step_execution.run_mode 对应")
    @ExcelProperty("执行模式，与 pe_step_execution.run_mode 对应")
    private String runMode;

    @Schema(description = "PIPELINE_STARTED | PIPELINE_PAUSED | PIPELINE_RESUMED | PIPELINE_SUCCESS | PIPELINE_FAILED | PIPELINE_CANCELLED | STEP_QUEUED | STEP_STARTED | STEP_SUCCESS | STEP_FAILED | STEP_SKIPPED | STEP_DEAD | STEP_RETRY_SCHEDULED | STEP_COMPENSATE_TRIGGERED | STEP_COMPENSATED | STANDALONE_STARTED | STANDALONE_FINISHED", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("PIPELINE_STARTED | PIPELINE_PAUSED | PIPELINE_RESUMED | PIPELINE_SUCCESS | PIPELINE_FAILED | PIPELINE_CANCELLED | STEP_QUEUED | STEP_STARTED | STEP_SUCCESS | STEP_FAILED | STEP_SKIPPED | STEP_DEAD | STEP_RETRY_SCHEDULED | STEP_COMPENSATE_TRIGGERED | STEP_COMPENSATED | STANDALONE_STARTED | STANDALONE_FINISHED")
    private String eventType;

    @Schema(description = "变更前状态", example = "1")
    @ExcelProperty("变更前状态")
    private String fromStatus;

    @Schema(description = "变更后状态", example = "2")
    @ExcelProperty("变更后状态")
    private String toStatus;

    @Schema(description = "事件附加数据，如错误原因、重试间隔、设备 ID、补偿执行 ID")
    @ExcelProperty("事件附加数据，如错误原因、重试间隔、设备 ID、补偿执行 ID")
    private String payload;

    @Schema(description = "操作人，系统触发记 SYSTEM")
    @ExcelProperty("操作人，系统触发记 SYSTEM")
    private String operator;

    @Schema(description = "事件发生时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("事件发生时间")
    private LocalDateTime occurredAt;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}