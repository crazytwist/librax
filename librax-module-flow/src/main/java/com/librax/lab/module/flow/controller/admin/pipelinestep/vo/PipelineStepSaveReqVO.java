package com.librax.lab.module.flow.controller.admin.pipelinestep.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]新增/修改 Request VO")
@Data
public class PipelineStepSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "29657")
    private Long id;

    @Schema(description = "关联 pd_pipeline_definition.pipeline_key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "关联 pd_pipeline_definition.pipeline_key不能为空")
    private String pipelineKey;

    @Schema(description = "关联 pd_pipeline_definition.version", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "关联 pd_pipeline_definition.version不能为空")
    private Integer pipelineVersion;

    @Schema(description = "节点 ID，同一流程版本内唯一，小写+下划线，如 s_ph", requiredMode = Schema.RequiredMode.REQUIRED, example = "5566")
    @NotEmpty(message = "节点 ID，同一流程版本内唯一，小写+下划线，如 s_ph不能为空")
    private String nodeId;

    @Schema(description = "关联 pd_step_definition.step_key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "关联 pd_step_definition.step_key不能为空")
    private String stepKey;

    @Schema(description = "指定步骤版本，NULL 表示始终用最新 ACTIVE 版本")
    private Integer stepVersion;

    @Schema(description = "前置节点 node_id 列表，NULL 或空数组表示入口节点")
    private String dependsOn;

    @Schema(description = "CONDITION 节点判断表达式，如 ${s_calc.score} >= 80")
    private String conditionExpr;

    @Schema(description = "CONDITION 节点 expr=true 时跳转的 node_id")
    private String trueBranch;

    @Schema(description = "CONDITION 节点 expr=false 时跳转的 node_id")
    private String falseBranch;

    @Schema(description = "覆盖 pd_step_definition.default_params，只填需要覆盖的字段")
    private String paramsOverride;

    @Schema(description = "从上下文引用前置节点输出")
    private String inputMapping;

    @Schema(description = "将执行返回值映射为语义字段")
    private String outputMapping;

    @Schema(description = "超时时间(ms)，NULL 则向上取默认值")
    private Long timeoutMs;

    @Schema(description = "最大重试次数，NULL 则向上取默认值")
    private Integer maxAttempts;

    @Schema(description = "重试退避时间(ms)，NULL 则向上取默认值")
    private Long backoffMs;

    @Schema(description = "FAIL_PIPELINE | SKIP | RETRY_ONLY | COMPENSATE，覆盖流程级 fail_strategy，NULL 则继承")
    private String onFailure;

    @Schema(description = "失败时触发的补偿节点 node_id，引用本流程内已有节点", example = "27145")
    private String compensateNodeId;

    @Schema(description = "失败时触发的补偿步骤 step_key，直接指定不依赖流程内已有节点")
    private String compensateStepKey;

    @Schema(description = "补偿步骤入参，支持 ${node_id.field} 引用上下文")
    private String compensateParams;

    @Schema(description = "ON_FAIL | ON_TIMEOUT | ALWAYS，NULL 继承流程级策略")
    private String compensateOn;

    @Schema(description = "是否支持单独运行，覆盖 pd_step_definition.runnable_standalone", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否支持单独运行，覆盖 pd_step_definition.runnable_standalone不能为空")
    private Boolean runnableStandalone;

    @Schema(description = "单独运行或测试时的 Mock 输出，覆盖 pd_step_definition.mock_output")
    private String mockOutput;

    @Schema(description = "前端展示排序，不影响调度逻辑")
    private Integer sortOrder;

    @Schema(description = "画布坐标")
    private String uiPosition;

}