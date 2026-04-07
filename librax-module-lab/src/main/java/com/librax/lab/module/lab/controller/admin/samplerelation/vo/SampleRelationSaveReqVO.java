package com.librax.lab.module.lab.controller.admin.samplerelation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 样本谱系关系表，记录拆分/合并/分装等衍生关系新增/修改 Request VO")
@Data
public class SampleRelationSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "27253")
    private Long id;

    @Schema(description = "当前样本ID（衍生出的新样本）", requiredMode = Schema.RequiredMode.REQUIRED, example = "21643")
    @NotEmpty(message = "当前样本ID（衍生出的新样本）不能为空")
    private String sampleId;

    @Schema(description = "关联样本ID（来源样本）", requiredMode = Schema.RequiredMode.REQUIRED, example = "27245")
    @NotEmpty(message = "关联样本ID（来源样本）不能为空")
    private String relatedSampleId;

    @Schema(description = "关系类型：SPLIT_FROM拆分来源 / MERGE_FROM合并来源 / ALIQUOT_FROM分装来源 / DERIVED_FROM衍生来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "关系类型：SPLIT_FROM拆分来源 / MERGE_FROM合并来源 / ALIQUOT_FROM分装来源 / DERIVED_FROM衍生来源不能为空")
    private String relationType;

    @Schema(description = "转移量(微升)，拆分/分装时记录从来源取了多少")
    private BigDecimal quantityUl;

    @Schema(description = "在哪次流程执行中发生的", example = "18574")
    private String executionId;

    @Schema(description = "在哪个步骤中发生的", example = "31264")
    private String nodeId;

    @Schema(description = "备注")
    private String remark;

}