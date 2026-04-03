package com.librax.lab.module.flow.controller.admin.executioncontext.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用新增/修改 Request VO")
@Data
public class ExecutionContextSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "18340")
    private Long id;

    @Schema(description = "关联 pe_pipeline_execution.execution_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "5932")
    @NotEmpty(message = "关联 pe_pipeline_execution.execution_id不能为空")
    private String executionId;

    @Schema(description = "已完成节点的输出汇总，key 为 node_id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "已完成节点的输出汇总，key 为 node_id")
    private String contextData;

}