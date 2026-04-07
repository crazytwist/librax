package com.librax.lab.module.lab.controller.admin.samplerelation.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 样本谱系关系表，记录拆分/合并/分装等衍生关系分页 Request VO")
@Data
public class SampleRelationPageReqVO extends PageParam {

    @Schema(description = "当前样本ID（衍生出的新样本）", example = "21643")
    private String sampleId;

    @Schema(description = "关联样本ID（来源样本）", example = "27245")
    private String relatedSampleId;

    @Schema(description = "关系类型：SPLIT_FROM拆分来源 / MERGE_FROM合并来源 / ALIQUOT_FROM分装来源 / DERIVED_FROM衍生来源", example = "1")
    private String relationType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}