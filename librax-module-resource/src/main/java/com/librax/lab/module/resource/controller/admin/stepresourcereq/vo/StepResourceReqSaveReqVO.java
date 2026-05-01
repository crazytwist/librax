package com.librax.lab.module.resource.controller.admin.stepresourcereq.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 步骤资源需求定义，一个步骤节点可配多行（一步多资源）新增/修改 Request VO")
@Data
public class StepResourceReqSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "23653")
    private Long id;

    @Schema(description = "关联 pd_pipeline_step.id，步骤节点级别的资源需求", requiredMode = Schema.RequiredMode.REQUIRED, example = "22112")
    @NotNull(message = "关联 pd_pipeline_step.id，步骤节点级别的资源需求不能为空")
    private Long pipelineStepId;

    @Schema(description = "需要的资源类型，如 PH_METER、AGV，对应 lab_resource_config.resource_type", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "需要的资源类型，如 PH_METER、AGV，对应 lab_resource_config.resource_type不能为空")
    private String resourceType;

    @Schema(description = "需要数量，通常为1，AGV等可能>1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "需要数量，通常为1，AGV等可能>1不能为空")
    private Integer quantity;

    @Schema(description = "1=独占（同时只能一个步骤用）0=共享（只读设备可并发）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "1=独占（同时只能一个步骤用）0=共享（只读设备可并发）不能为空")
    private Boolean isExclusive;

    @Schema(description = "全局统一申请顺序，所有步骤必须按相同顺序申请，防死锁", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "全局统一申请顺序，所有步骤必须按相同顺序申请，防死锁不能为空")
    private Integer sortOrder;

}