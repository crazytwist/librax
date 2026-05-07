package com.librax.lab.module.flow.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 执行概览")
@Data
public class ExecutionOverviewRespVO {

    @Schema(description = "运行中数量")
    private long runningCount;

    @Schema(description = "等待中数量")
    private long pendingCount;

    @Schema(description = "已暂停数量")
    private long pausedCount;

    @Schema(description = "成功数量")
    private long successCount;

    @Schema(description = "失败数量")
    private long failedCount;
}
