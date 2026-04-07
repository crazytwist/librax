package com.librax.lab.module.lab.controller.admin.sampleresult.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]新增/修改 Request VO")
@Data
public class SampleResultSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "15930")
    private Long id;

    @Schema(description = "结果业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "17772")
    @NotEmpty(message = "结果业务ID不能为空")
    private String resultId;

    @Schema(description = "样本ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3524")
    @NotEmpty(message = "样本ID不能为空")
    private String sampleId;

    @Schema(description = "根样本ID，冗余存便于溯源统计", example = "3699")
    private String rootSampleId;

    @Schema(description = "批次号，冗余存便于批次统计")
    private String batchNo;

    @Schema(description = "产生该结果的流程执行ID", example = "2184")
    private String executionId;

    @Schema(description = "产生该结果的步骤节点ID", example = "23155")
    private String nodeId;

    @Schema(description = "检测项目代码：PH/TURBIDITY/COD/BOD/WBC/HGB等", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "检测项目代码：PH/TURBIDITY/COD/BOD/WBC/HGB等不能为空")
    private String testItem;

    @Schema(description = "检测项目名称：酸碱度/浊度/化学需氧量等", example = "李四")
    private String testItemName;

    @Schema(description = "检测方法/标准")
    private String testMethod;

    @Schema(description = "检测结果数值（定量结果）")
    private BigDecimal resultValue;

    @Schema(description = "非数值结果（定性结果）：阳性/阴性/未检出/+++等")
    private String resultText;

    @Schema(description = "单位：mg/L、NTU、pH、×10^9/L等")
    private String unit;

    @Schema(description = "结果精度（小数位数），用于显示")
    private Integer precisionDigits;

    @Schema(description = "参考范围下限")
    private BigDecimal referenceLow;

    @Schema(description = "参考范围上限")
    private BigDecimal referenceHigh;

    @Schema(description = "参考范围描述")
    private String referenceText;

    @Schema(description = "是否异常（超出参考范围）")
    private Boolean isAbnormal;

    @Schema(description = "异常标记：HIGH偏高 / LOW偏低 / CRITICAL危急值 / POSITIVE阳性")
    private String abnormalFlag;

    @Schema(description = "检测设备ID", example = "11392")
    private String deviceId;

    @Schema(description = "检测设备名称，冗余存便于展示", example = "芋艿")
    private String deviceName;

    @Schema(description = "实际检测时间（设备上报时间）")
    private LocalDateTime measuredAt;

    @Schema(description = "设备返回的原始报文/数据")
    private String rawData;

    @Schema(description = "设备原始值（未经换算的）")
    private String rawValue;

    @Schema(description = "PENDING待审核 / AUTO_APPROVED自动通过 / APPROVED人工通过 / REJECTED驳回 / RETEST需复测", example = "1")
    private String reviewStatus;

    @Schema(description = "审核人")
    private String reviewedBy;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    @Schema(description = "审核意见")
    private String reviewComment;

    @Schema(description = "复测流程执行ID，RETEST时关联", example = "32048")
    private String retestExecutionId;

    @Schema(description = "复测结果ID，关联新的结果记录", example = "4459")
    private String retestResultId;

    @Schema(description = "是否最终结果，复测后原结果标记为0")
    private Boolean isFinal;

    @Schema(description = "备注", example = "你猜")
    private String remark;

}