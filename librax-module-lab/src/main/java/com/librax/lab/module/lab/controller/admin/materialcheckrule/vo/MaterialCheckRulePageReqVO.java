package com.librax.lab.module.lab.controller.admin.materialcheckrule.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理分页 Request VO")
@Data
public class MaterialCheckRulePageReqVO extends PageParam {

    @Schema(description = "关联 pd_pipeline_step.id，步骤节点级别（不是步骤模板级别）", example = "7874")
    private Long pipelineStepId;

    @Schema(description = "需要检查的内容物编码，关联 lab_material_def.material_code")
    private String materialCode;

    @Schema(description = "内容物类型（可选），和 material_code 二选一或组合使用。只填类型时检查该类型任意物料", example = "1")
    private String contentType;

    @Schema(description = "最低体积要求（微升），液体类填写，不足时阻止执行")
    private BigDecimal minVolUl;

    @Schema(description = "最低数量要求（个），固体/耗材类填写", example = "8824")
    private Integer minCount;

    @Schema(description = "指定从哪个区域取用，NULL表示不限区域")
    private String zoneCode;

    @Schema(description = "检查顺序（同一步骤有多种物料需求时的检查顺序）")
    private Integer sortOrder;

    @Schema(description = "备注说明，如 此步骤消耗约50ul缓冲液", example = "随便")
    private String remark;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}