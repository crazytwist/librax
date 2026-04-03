package com.librax.lab.module.flow.controller.admin.executioncontext.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ExecutionContextRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "18340")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "关联 pe_pipeline_execution.execution_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "5932")
    @ExcelProperty("关联 pe_pipeline_execution.execution_id")
    private String executionId;

    @Schema(description = "已完成节点的输出汇总，key 为 node_id", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("已完成节点的输出汇总，key 为 node_id }")
    private String contextData;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}