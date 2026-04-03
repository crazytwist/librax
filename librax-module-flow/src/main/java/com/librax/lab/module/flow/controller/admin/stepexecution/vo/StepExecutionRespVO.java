package com.librax.lab.module.flow.controller.admin.stepexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪 Response VO")
@Data
@ExcelIgnoreUnannotated
public class StepExecutionRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "21044")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "关联 pe_pipeline_execution.execution_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "2067")
    @ExcelProperty("关联 pe_pipeline_execution.execution_id")
    private String executionId;

    @Schema(description = "节点 ID，对应 pd_pipeline_step.node_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "6933")
    @ExcelProperty("节点 ID，对应 pd_pipeline_step.node_id")
    private String nodeId;

    @Schema(description = "冗余步骤标识，对应 pd_step_definition.step_key，便于按类型统计")
    @ExcelProperty("冗余步骤标识，对应 pd_step_definition.step_key，便于按类型统计")
    private String stepKey;

    @Schema(description = "INSTRUMENT | COMPUTE | CONDITION | WAIT | NOTIFY", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("INSTRUMENT | COMPUTE | CONDITION | WAIT | NOTIFY")
    private String stepType;

    @Schema(description = "第几次尝试，1-based，每次重试 INSERT 新行不覆盖历史", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("第几次尝试，1-based，每次重试 INSERT 新行不覆盖历史")
    private Integer attempt;

    @Schema(description = "PENDING | RUNNING | SUCCESS | FAILED | SKIPPED | DEAD | COMPENSATING | COMPENSATED", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("PENDING | RUNNING | SUCCESS | FAILED | SKIPPED | DEAD | COMPENSATING | COMPENSATED")
    private String status;

    @Schema(description = "NORMAL | STANDALONE | COMPENSATE | MOCK", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("NORMAL | STANDALONE | COMPENSATE | MOCK")
    private String runMode;

    @Schema(description = "入参快照，input_mapping 解析后的实际值，执行前打点")
    @ExcelProperty("入参快照，input_mapping 解析后的实际值，执行前打点")
    private String inputSnapshot;

    @Schema(description = "步骤输出，SUCCESS 后写入上下文的字段")
    @ExcelProperty("步骤输出，SUCCESS 后写入上下文的字段")
    private String outputData;

    @Schema(description = "错误码，如 DEVICE_TIMEOUT | COMPUTE_ERROR")
    @ExcelProperty("错误码，如 DEVICE_TIMEOUT | COMPUTE_ERROR")
    private String errorCode;

    @Schema(description = "错误详情")
    @ExcelProperty("错误详情")
    private String errorMsg;

    @Schema(description = "进入 PENDING 时打点")
    @ExcelProperty("进入 PENDING 时打点")
    private LocalDateTime queuedAt;

    @Schema(description = "进入 RUNNING 时打点")
    @ExcelProperty("进入 RUNNING 时打点")
    private LocalDateTime startedAt;

    @Schema(description = "进入终态时打点")
    @ExcelProperty("进入终态时打点")
    private LocalDateTime finishedAt;

    @Schema(description = "排队等待耗时(ms) = started_at - queued_at")
    @ExcelProperty("排队等待耗时(ms) = started_at - queued_at")
    private Long waitMs;

    @Schema(description = "实际执行耗时(ms) = finished_at - started_at")
    @ExcelProperty("实际执行耗时(ms) = finished_at - started_at")
    private Long executeMs;

    @Schema(description = "实际分配到的设备 ID，step_type=INSTRUMENT 时填写", example = "23921")
    @ExcelProperty("实际分配到的设备 ID，step_type=INSTRUMENT 时填写")
    private String deviceId;

    @Schema(description = "本节点触发的补偿执行 ID，DEAD 且触发补偿时填写", example = "23885")
    @ExcelProperty("本节点触发的补偿执行 ID，DEAD 且触发补偿时填写")
    private String compensateExecutionId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}