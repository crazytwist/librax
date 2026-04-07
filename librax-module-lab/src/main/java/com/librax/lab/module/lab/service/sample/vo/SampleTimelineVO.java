package com.librax.lab.module.lab.service.sample.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 样本事件时间线
 */
@Data
public class SampleTimelineVO {
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private String executionId;
    private String nodeId;
    private String locationFrom;
    private String locationTo;
    private BigDecimal volumeBefore;
    private BigDecimal volumeAfter;
    private String deviceId;
    private String operator;
    private LocalDateTime occurredAt;
    private String remark;
    private Map<String, Object> payload;
}