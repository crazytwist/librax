package com.librax.lab.module.flow.api.material;

import lombok.Builder;
import lombok.Data;

/**
 * 物料核验请求
 */
@Data
@Builder
public class MaterialCheckRequest {

    /** pd_pipeline_step.id，用于查关联的检查规则 */
    private Long pipelineStepId;

    /** 执行实例ID（日志追踪用） */
    private String executionId;

    /** 节点ID（日志追踪用） */
    private String nodeId;

    /** 步骤所在区域（如果规则里没指定 zoneCode，退回使用步骤级的 zoneCode） */
    private String zoneCode;
}
