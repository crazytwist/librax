package com.librax.lab.module.lab.controller.admin.samplerelation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 样本谱系关系表，记录拆分/合并/分装等衍生关系 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SampleRelationRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "27253")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "当前样本ID（衍生出的新样本）", requiredMode = Schema.RequiredMode.REQUIRED, example = "21643")
    @ExcelProperty("当前样本ID（衍生出的新样本）")
    private String sampleId;

    @Schema(description = "关联样本ID（来源样本）", requiredMode = Schema.RequiredMode.REQUIRED, example = "27245")
    @ExcelProperty("关联样本ID（来源样本）")
    private String relatedSampleId;

    @Schema(description = "关系类型：SPLIT_FROM拆分来源 / MERGE_FROM合并来源 / ALIQUOT_FROM分装来源 / DERIVED_FROM衍生来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("关系类型：SPLIT_FROM拆分来源 / MERGE_FROM合并来源 / ALIQUOT_FROM分装来源 / DERIVED_FROM衍生来源")
    private String relationType;

    @Schema(description = "转移量(微升)，拆分/分装时记录从来源取了多少")
    @ExcelProperty("转移量(微升)，拆分/分装时记录从来源取了多少")
    private BigDecimal quantityUl;

    @Schema(description = "在哪次流程执行中发生的", example = "18574")
    @ExcelProperty("在哪次流程执行中发生的")
    private String executionId;

    @Schema(description = "在哪个步骤中发生的", example = "31264")
    @ExcelProperty("在哪个步骤中发生的")
    private String nodeId;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}