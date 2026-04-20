package com.librax.lab.module.task.controller.admin.taskexecutorconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]新增/修改 Request VO")
@Data
public class TaskExecutorConfigSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "17519")
    private Long id;

    @Schema(description = "执行器类型：INSTRUMENT/AGV/COMPUTE/MANUAL", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "执行器类型：INSTRUMENT/AGV/COMPUTE/MANUAL不能为空")
    private String executorType;

    @Schema(description = "最大并发任务数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "最大并发任务数不能为空")
    private Integer maxConcurrent;

    @Schema(description = "队列容量，超出则拒绝新任务", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "队列容量，超出则拒绝新任务不能为空")
    private Integer queueCapacity;

    @Schema(description = "任务默认超时(ms)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务默认超时(ms)不能为空")
    private Long taskTimeoutMs;

    @Schema(description = "重试退避时间(ms)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "重试退避时间(ms)不能为空")
    private Long retryBackoffMs;

    @Schema(description = "是否可用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否可用不能为空")
    private Boolean enabled;

    @Schema(description = "备注", example = "你猜")
    private String remark;

}