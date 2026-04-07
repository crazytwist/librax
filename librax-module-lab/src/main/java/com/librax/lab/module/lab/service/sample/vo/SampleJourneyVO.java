package com.librax.lab.module.lab.service.sample.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 样本旅程（经过的每个步骤）
 */
@Data
public class SampleJourneyVO {
    private String executionId;
    private String nodeId;
    private String stepKey;
    private String stepType;
    private int attempt;
    private String status;         // BOUND / PROCESSING / COMPLETED / FAILED
    private String bindType;       // AUTO / MANUAL / SCAN
    private String deviceId;
    private String position;
    private Integer seqNo;
    private LocalDateTime boundAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Map<String, Object> resultData;   // 该步骤的检测结果
    private List<SampleJourneyResultVO> results;  // 结构化检测结果
}