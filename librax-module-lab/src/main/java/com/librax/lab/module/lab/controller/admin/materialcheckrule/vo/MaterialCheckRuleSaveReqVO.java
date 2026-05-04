package com.librax.lab.module.lab.controller.admin.materialcheckrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理新增/修改 Request VO")
@Data
public class MaterialCheckRuleSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "23438")
    private Long id;

    @Schema(description = "关联 pd_pipeline_step.id，步骤节点级别（不是步骤模板级别）", requiredMode = Schema.RequiredMode.REQUIRED, example = "7874")
    @NotNull(message = "关联 pd_pipeline_step.id，步骤节点级别（不是步骤模板级别）不能为空")
    private Long pipelineStepId;

    @Schema(description = "需要检查的内容物编码，关联 lab_material_def.material_code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "需要检查的内容物编码，关联 lab_material_def.material_code不能为空")
    private String materialCode;

    @Schema(description = "内容物类型（可选），和 material_code 二选一或组合使用。只填类型时检查该类型任意物料", example = "1")
    private String contentType;

    @Schema(description = "最低体积要求（微升），液体类填写，不足时阻止执行")
    private BigDecimal minVolUl;

    @Schema(description = "最低数量要求（个），固体/耗材类填写", example = "8824")
    private Integer minCount;

    @Schema(description = "指定从哪个区域取用，NULL表示不限区域")
    private String zoneCode;

    @Schema(description = "检查顺序（同一步骤有多种物料需求时的检查顺序）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检查顺序（同一步骤有多种物料需求时的检查顺序）不能为空")
    private Integer sortOrder;

    @Schema(description = "备注说明，如 此步骤消耗约50ul缓冲液", example = "随便")
    private String remark;

}