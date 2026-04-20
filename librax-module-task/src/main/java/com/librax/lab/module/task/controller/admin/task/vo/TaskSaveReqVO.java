package com.librax.lab.module.task.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]新增/修改 Request VO")
@Data
public class TaskSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "15104")
    private Long id;

    @Schema(description = "任务唯一业务ID，T-{雪花}", requiredMode = Schema.RequiredMode.REQUIRED, example = "531")
    @NotEmpty(message = "任务唯一业务ID，T-{雪花}不能为空")
    private String taskId;

    @Schema(description = "任务类型：INSTRUMENT/AGV/COMPUTE/MANUAL/NOTIFY", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "任务类型：INSTRUMENT/AGV/COMPUTE/MANUAL/NOTIFY不能为空")
    private String taskType;

    @Schema(description = "任务名称，便于展示，如 PH检测-执行", example = "张三")
    private String taskName;

    @Schema(description = "关联 pe_pipeline_execution.execution_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "30048")
    @NotEmpty(message = "关联 pe_pipeline_execution.execution_id不能为空")
    private String executionId;

    @Schema(description = "关联 pd_pipeline_step.node_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "14089")
    @NotEmpty(message = "关联 pd_pipeline_step.node_id不能为空")
    private String nodeId;

    @Schema(description = "冗余步骤标识，便于统计")
    private String stepKey;

    @Schema(description = "关联样本ID，单样本任务填写", example = "29068")
    private String sampleId;

    @Schema(description = "批次号，冗余存便于批次维度统计")
    private String batchNo;

    @Schema(description = "优先级：0普通 1加急 2特急，跨类型统一排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "优先级：0普通 1加急 2特急，跨类型统一排序不能为空")
    private Integer priority;

    @Schema(description = "期望执行区域，影响执行器选择")
    private String zoneCode;

    @Schema(description = "期望最早执行时间，NULL表示立即")
    private LocalDateTime scheduledAt;

    @Schema(description = "截止时间，超时未完成触发告警")
    private LocalDateTime deadlineAt;

    @Schema(description = "被分配的执行单元ID，如 deviceId/agvId", example = "15726")
    private String executorId;

    @Schema(description = "执行单元类型：DEVICE/AGV/BEAN/HUMAN", example = "1")
    private String executorType;

    @Schema(description = "外部系统任务ID，如设备侧taskId/AGV调度端jobId", example = "15091")
    private String externalTaskId;

    @Schema(description = "PENDING/ASSIGNED/EXECUTING/DONE/FAILED/CANCELLED/TIMEOUT", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "PENDING/ASSIGNED/EXECUTING/DONE/FAILED/CANCELLED/TIMEOUT不能为空")
    private String status;

    @Schema(description = "失败原因简述", example = "不对")
    private String failReason;

    @Schema(description = "当前已重试次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "25801")
    @NotNull(message = "当前已重试次数不能为空")
    private Integer retryCount;

    @Schema(description = "最大重试次数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "最大重试次数不能为空")
    private Integer maxRetry;

    @Schema(description = "回调令牌，执行完成时校验用，与 pe_step_execution 一致", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "回调令牌，执行完成时校验用，与 pe_step_execution 一致不能为空")
    private String callbackToken;

    @Schema(description = "执行参数，各任务类型自定义结构")
    private String payload;

    @Schema(description = "执行结果输出，成功后写入，回调时带给引擎")
    private String result;

    @Schema(description = "错误码，失败时填写")
    private String errorCode;

    @Schema(description = "错误详情")
    private String errorMsg;

    @Schema(description = "进入队列时间")
    private LocalDateTime queuedAt;

    @Schema(description = "分配给执行单元时间")
    private LocalDateTime assignedAt;

    @Schema(description = "开始执行时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间（终态）")
    private LocalDateTime finishedAt;

    @Schema(description = "排队等待耗时 = assigned_at - queued_at")
    private Long waitMs;

    @Schema(description = "实际执行耗时 = finished_at - started_at")
    private Long executeMs;

}