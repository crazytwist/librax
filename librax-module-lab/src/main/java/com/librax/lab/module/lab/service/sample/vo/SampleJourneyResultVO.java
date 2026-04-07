package com.librax.lab.module.lab.service.sample.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 旅程中的检测结果
 */
@Data
public class SampleJourneyResultVO {
    private String resultId;
    private String testItem;
    private String testItemName;
    private BigDecimal resultValue;
    private String resultText;
    private String unit;
    private String referenceText;
    private Boolean isAbnormal;
    private String abnormalFlag;
    private String reviewStatus;
}