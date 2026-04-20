package com.librax.lab.module.task.controller.admin.taskevent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]新增/修改 Request VO")
@Data
public class TaskEventSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "20077")
    private Long id;

    @Schema(description = "关联 lab_task.task_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "28100")
    @NotEmpty(message = "关联 lab_task.task_id不能为空")
    private String taskId;

    @Schema(description = "冗余，便于按类型查日志", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "冗余，便于按类型查日志不能为空")
    private String taskType;

    @Schema(description = "CREATED/ASSIGNED/STARTED/DONE/FAILED/RETRYING/CANCELLED/TIMEOUT", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "CREATED/ASSIGNED/STARTED/DONE/FAILED/RETRYING/CANCELLED/TIMEOUT不能为空")
    private String eventType;

    @Schema(description = "初始状态", example = "2")
    private String fromStatus;

    @Schema(description = "结束状态", example = "2")
    private String toStatus;

    @Schema(description = "操作的执行单元", example = "29961")
    private String executorId;

    @Schema(description = "附加信息，如分配原因、重试次数、错误详情")
    private String payload;

    @Schema(description = "操作人，系统触发记 SYSTEM")
    private String operator;

    @Schema(description = "发生事件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发生事件不能为空")
    private LocalDateTime occurredAt;

}