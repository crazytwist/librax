package com.librax.lab.module.resource.controller.admin.stepresourcehold.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 步骤执行资源占用记录，released_at IS NULL 表示当前持有中新增/修改 Request VO")
@Data
public class StepResourceHoldSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "14716")
    private Long id;

    @Schema(description = "关联 pe_pipeline_execution.execution_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "22370")
    @NotEmpty(message = "关联 pe_pipeline_execution.execution_id不能为空")
    private String executionId;

    @Schema(description = "关联 pd_pipeline_step.node_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "9863")
    @NotEmpty(message = "关联 pd_pipeline_step.node_id不能为空")
    private String nodeId;

    @Schema(description = "第几次重试，与 pe_step_execution.attempt 对应", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "第几次重试，与 pe_step_execution.attempt 对应不能为空")
    private Integer attempt;

    @Schema(description = "实际占用的具体设备ID，关联 lab_resource_config.resource_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "12278")
    @NotEmpty(message = "实际占用的具体设备ID，关联 lab_resource_config.resource_id不能为空")
    private String resourceId;

    @Schema(description = "资源类型，冗余存便于查询", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "资源类型，冗余存便于查询不能为空")
    private String resourceType;

    @Schema(description = "申请到资源的时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请到资源的时间不能为空")
    private LocalDateTime acquiredAt;

    @Schema(description = "NULL=当前持有中，有值=已释放")
    private LocalDateTime releasedAt;

    @Schema(description = "STEP_COMPLETE / STEP_DEAD / TIMEOUT / CANCELLED / RECOVERY", example = "不对")
    private String releaseReason;

}