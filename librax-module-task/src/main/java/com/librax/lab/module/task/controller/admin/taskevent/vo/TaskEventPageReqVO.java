package com.librax.lab.module.task.controller.admin.taskevent.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]分页 Request VO")
@Data
public class TaskEventPageReqVO extends PageParam {

    @Schema(description = "关联 lab_task.task_id", example = "28100")
    private String taskId;

    @Schema(description = "冗余，便于按类型查日志", example = "2")
    private String taskType;

    @Schema(description = "CREATED/ASSIGNED/STARTED/DONE/FAILED/RETRYING/CANCELLED/TIMEOUT", example = "2")
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

    @Schema(description = "发生事件")
    private LocalDateTime occurredAt;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}