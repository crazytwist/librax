package com.librax.lab.module.flow.controller.admin.stepexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪新增/修改 Request VO")
@Data
public class StepExecutionSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "21044")
    private Long id;

    @Schema(description = "关联 pe_pipeline_execution.execution_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "2067")
    @NotEmpty(message = "关联 pe_pipeline_execution.execution_id不能为空")
    private String executionId;

    @Schema(description = "节点 ID，对应 pd_pipeline_step.node_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "6933")
    @NotEmpty(message = "节点 ID，对应 pd_pipeline_step.node_id不能为空")
    private String nodeId;

    @Schema(description = "冗余步骤标识，对应 pd_step_definition.step_key，便于按类型统计")
    private String stepKey;

    @Schema(description = "INSTRUMENT | COMPUTE | CONDITION | WAIT | NOTIFY", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "INSTRUMENT | COMPUTE | CONDITION | WAIT | NOTIFY不能为空")
    private String stepType;

    @Schema(description = "第几次尝试，1-based，每次重试 INSERT 新行不覆盖历史", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "第几次尝试，1-based，每次重试 INSERT 新行不覆盖历史不能为空")
    private Integer attempt;

    @Schema(description = "PENDING | RUNNING | SUCCESS | FAILED | SKIPPED | DEAD | COMPENSATING | COMPENSATED", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "PENDING | RUNNING | SUCCESS | FAILED | SKIPPED | DEAD | COMPENSATING | COMPENSATED不能为空")
    private String status;

    @Schema(description = "NORMAL | STANDALONE | COMPENSATE | MOCK", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "NORMAL | STANDALONE | COMPENSATE | MOCK不能为空")
    private String runMode;

    @Schema(description = "入参快照，input_mapping 解析后的实际值，执行前打点")
    private String inputSnapshot;

    @Schema(description = "步骤输出，SUCCESS 后写入上下文的字段")
    private String outputData;

    @Schema(description = "错误码，如 DEVICE_TIMEOUT | COMPUTE_ERROR")
    private String errorCode;

    @Schema(description = "错误详情")
    private String errorMsg;

    @Schema(description = "进入 PENDING 时打点")
    private LocalDateTime queuedAt;

    @Schema(description = "进入 RUNNING 时打点")
    private LocalDateTime startedAt;

    @Schema(description = "进入终态时打点")
    private LocalDateTime finishedAt;

    @Schema(description = "排队等待耗时(ms) = started_at - queued_at")
    private Long waitMs;

    @Schema(description = "实际执行耗时(ms) = finished_at - started_at")
    private Long executeMs;

    @Schema(description = "实际分配到的设备 ID，step_type=INSTRUMENT 时填写", example = "23921")
    private String deviceId;

    @Schema(description = "本节点触发的补偿执行 ID，DEAD 且触发补偿时填写", example = "23885")
    private String compensateExecutionId;

}