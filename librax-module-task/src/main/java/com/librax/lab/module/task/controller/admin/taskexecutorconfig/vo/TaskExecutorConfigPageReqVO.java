package com.librax.lab.module.task.controller.admin.taskexecutorconfig.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]分页 Request VO")
@Data
public class TaskExecutorConfigPageReqVO extends PageParam {

    @Schema(description = "执行器类型：INSTRUMENT/AGV/COMPUTE/MANUAL", example = "1")
    private String executorType;

    @Schema(description = "最大并发任务数")
    private Integer maxConcurrent;

    @Schema(description = "队列容量，超出则拒绝新任务")
    private Integer queueCapacity;

    @Schema(description = "任务默认超时(ms)")
    private Long taskTimeoutMs;

    @Schema(description = "重试退避时间(ms)")
    private Long retryBackoffMs;

    @Schema(description = "是否可用")
    private Boolean enabled;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}