package com.librax.lab.module.flow.controller.admin.pipelinedefinition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]新增/修改 Request VO")
@Data
public class PipelineDefinitionSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "24282")
    private Long id;

    @Schema(description = "流程唯一标识，小写+下划线，如 water_quality_test", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "流程唯一标识，小写+下划线，如 water_quality_test不能为空")
    private String pipelineKey;

    @Schema(description = "版本号，同 key 下从 1 递增", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "版本号，同 key 下从 1 递增不能为空")
    private Integer version;

    @Schema(description = "流程显示名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "流程显示名称不能为空")
    private String name;

    @Schema(description = "流程说明", example = "随便")
    private String description;

    @Schema(description = "FAIL_FAST 任意节点失败即终止 | CONTINUE_ON_FAIL 跳过失败节点继续执行", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "FAIL_FAST 任意节点失败即终止 | CONTINUE_ON_FAIL 跳过失败节点继续执行不能为空")
    private String failStrategy;

    @Schema(description = "NONE 不补偿 | ON_FAIL 失败时触发 | ALWAYS 无论成败都触发", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "NONE 不补偿 | ON_FAIL 失败时触发 | ALWAYS 无论成败都触发不能为空")
    private String compensateStrategy;

    @Schema(description = "流程级默认节点超时(ms)，可被 pd_pipeline_step 覆盖")
    private Long defaultTimeoutMs;

    @Schema(description = "流程级默认最大重试次数，可被 pd_pipeline_step 覆盖")
    private Integer defaultMaxAttempts;

    @Schema(description = "流程级默认退避时间(ms)，可被 pd_pipeline_step 覆盖")
    private Long defaultBackoffMs;

    @Schema(description = "DRAFT 草稿 | ACTIVE 已发布 | DISABLED 已停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "DRAFT 草稿 | ACTIVE 已发布 | DISABLED 已停用不能为空")
    private String status;

    @Schema(description = "发布时间，status=ACTIVE 时填写")
    private LocalDateTime publishedAt;

}