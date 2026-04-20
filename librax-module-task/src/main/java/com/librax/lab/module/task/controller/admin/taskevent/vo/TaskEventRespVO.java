package com.librax.lab.module.task.controller.admin.taskevent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_] Response VO")
@Data
@ExcelIgnoreUnannotated
public class TaskEventRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "20077")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "关联 lab_task.task_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "28100")
    @ExcelProperty("关联 lab_task.task_id")
    private String taskId;

    @Schema(description = "冗余，便于按类型查日志", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("冗余，便于按类型查日志")
    private String taskType;

    @Schema(description = "CREATED/ASSIGNED/STARTED/DONE/FAILED/RETRYING/CANCELLED/TIMEOUT", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("CREATED/ASSIGNED/STARTED/DONE/FAILED/RETRYING/CANCELLED/TIMEOUT")
    private String eventType;

    @Schema(description = "初始状态", example = "2")
    @ExcelProperty("初始状态")
    private String fromStatus;

    @Schema(description = "结束状态", example = "2")
    @ExcelProperty("结束状态")
    private String toStatus;

    @Schema(description = "操作的执行单元", example = "29961")
    @ExcelProperty("操作的执行单元")
    private String executorId;

    @Schema(description = "附加信息，如分配原因、重试次数、错误详情")
    @ExcelProperty("附加信息，如分配原因、重试次数、错误详情")
    private String payload;

    @Schema(description = "操作人，系统触发记 SYSTEM")
    @ExcelProperty("操作人，系统触发记 SYSTEM")
    private String operator;

    @Schema(description = "发生事件", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("发生事件")
    private LocalDateTime occurredAt;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}