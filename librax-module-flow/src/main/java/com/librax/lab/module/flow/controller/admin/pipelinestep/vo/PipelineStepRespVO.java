package com.librax.lab.module.flow.controller.admin.pipelinestep.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_] Response VO")
@Data
@ExcelIgnoreUnannotated
public class PipelineStepRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "29657")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "关联 pd_pipeline_definition.pipeline_key", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("关联 pd_pipeline_definition.pipeline_key")
    private String pipelineKey;

    @Schema(description = "关联 pd_pipeline_definition.version", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("关联 pd_pipeline_definition.version")
    private Integer pipelineVersion;

    @Schema(description = "节点 ID，同一流程版本内唯一，小写+下划线，如 s_ph", requiredMode = Schema.RequiredMode.REQUIRED, example = "5566")
    @ExcelProperty("节点 ID，同一流程版本内唯一，小写+下划线，如 s_ph")
    private String nodeId;

    @Schema(description = "关联 pd_step_definition.step_key", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("关联 pd_step_definition.step_key")
    private String stepKey;

    @Schema(description = "指定步骤版本，NULL 表示始终用最新 ACTIVE 版本")
    @ExcelProperty("指定步骤版本，NULL 表示始终用最新 ACTIVE 版本")
    private Integer stepVersion;

    @Schema(description = "前置节点 node_id 列表，NULL 或空数组表示入口节点")
    @ExcelProperty("前置节点 node_id 列表，NULL 或空数组表示入口节点，如 ")
    private String dependsOn;

    @Schema(description = "CONDITION 节点判断表达式，如 ${s_calc.score} >= 80")
    @ExcelProperty("CONDITION 节点判断表达式，如 ${s_calc.score} >= 80")
    private String conditionExpr;

    @Schema(description = "CONDITION 节点 expr=true 时跳转的 node_id")
    @ExcelProperty("CONDITION 节点 expr=true 时跳转的 node_id")
    private String trueBranch;

    @Schema(description = "CONDITION 节点 expr=false 时跳转的 node_id")
    @ExcelProperty("CONDITION 节点 expr=false 时跳转的 node_id")
    private String falseBranch;

    @Schema(description = "覆盖 pd_step_definition.default_params，只填需要覆盖的字段")
    @ExcelProperty("覆盖 pd_step_definition.default_params，只填需要覆盖的字段")
    private String paramsOverride;

    @Schema(description = "从上下文引用前置节点输出")
    @ExcelProperty("从上下文引用前置节点输出，如 ")
    private String inputMapping;

    @Schema(description = "将执行返回值映射为语义字段 ")
    @ExcelProperty("将执行返回值映射为语义字段")
    private String outputMapping;

    @Schema(description = "超时时间(ms)，NULL 则向上取默认值")
    @ExcelProperty("超时时间(ms)，NULL 则向上取默认值")
    private Long timeoutMs;

    @Schema(description = "最大重试次数，NULL 则向上取默认值")
    @ExcelProperty("最大重试次数，NULL 则向上取默认值")
    private Integer maxAttempts;

    @Schema(description = "重试退避时间(ms)，NULL 则向上取默认值")
    @ExcelProperty("重试退避时间(ms)，NULL 则向上取默认值")
    private Long backoffMs;

    @Schema(description = "FAIL_FAST | SKIP | CONTINUE_ON_FAIL，覆盖流程级 fail_strategy，NULL 则继承")
    @ExcelProperty("FAIL_FAST | SKIP | CONTINUE_ON_FAIL，覆盖流程级 fail_strategy，NULL 则继承")
    private String onFailure;

    @Schema(description = "失败时触发的补偿节点 node_id，引用本流程内已有节点", example = "27145")
    @ExcelProperty("失败时触发的补偿节点 node_id，引用本流程内已有节点")
    private String compensateNodeId;

    @Schema(description = "失败时触发的补偿步骤 step_key，直接指定不依赖流程内已有节点")
    @ExcelProperty("失败时触发的补偿步骤 step_key，直接指定不依赖流程内已有节点")
    private String compensateStepKey;

    @Schema(description = "补偿步骤入参，支持 ${node_id.field} 引用上下文")
    @ExcelProperty("补偿步骤入参，支持 ${node_id.field} 引用上下文")
    private String compensateParams;

    @Schema(description = "ON_FAIL | ON_TIMEOUT | ALWAYS，NULL 继承流程级策略")
    @ExcelProperty("ON_FAIL | ON_TIMEOUT | ALWAYS，NULL 继承流程级策略")
    private String compensateOn;

    @Schema(description = "是否支持单独运行，覆盖 pd_step_definition.runnable_standalone", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否支持单独运行，覆盖 pd_step_definition.runnable_standalone")
    private Boolean runnableStandalone;

    @Schema(description = "单独运行或测试时的 Mock 输出，覆盖 pd_step_definition.mock_output")
    @ExcelProperty("单独运行或测试时的 Mock 输出，覆盖 pd_step_definition.mock_output")
    private String mockOutput;

    @Schema(description = "前端展示排序，不影响调度逻辑")
    @ExcelProperty("前端展示排序，不影响调度逻辑")
    private Integer sortOrder;

    @Schema(description = "画布坐标")
    @ExcelProperty("画布坐标")
    private String uiPosition;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}