package com.librax.lab.module.task.controller.admin.taskexecutorconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_] Response VO")
@Data
@ExcelIgnoreUnannotated
public class TaskExecutorConfigRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "17519")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "执行器类型：INSTRUMENT/AGV/COMPUTE/MANUAL", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("执行器类型：INSTRUMENT/AGV/COMPUTE/MANUAL")
    private String executorType;

    @Schema(description = "最大并发任务数", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("最大并发任务数")
    private Integer maxConcurrent;

    @Schema(description = "队列容量，超出则拒绝新任务", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("队列容量，超出则拒绝新任务")
    private Integer queueCapacity;

    @Schema(description = "任务默认超时(ms)", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("任务默认超时(ms)")
    private Long taskTimeoutMs;

    @Schema(description = "重试退避时间(ms)", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("重试退避时间(ms)")
    private Long retryBackoffMs;

    @Schema(description = "是否可用", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否可用")
    private Boolean enabled;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}