package com.librax.lab.module.lab.service.sample.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SampleInfoSummaryVO {
    private String sampleId;
    private String rootSampleId;
    private String parentSampleId;
    private String sampleType;
    private String sampleName;
    private String deriveType;
    private Integer generation;
    private String status;
    private BigDecimal volumeUl;
    private BigDecimal initialVolumeUl;
    private String containerType;
    private String containerCode;
    private String locationCode;
    private String batchNo;
    private String orderNo;
    private Integer priority;
    private String source;
    private String externalId;
    private LocalDateTime collectedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}