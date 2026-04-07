package com.librax.lab.module.lab.service.sample.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 样本谱系树节点
 * 递归结构：每个节点包含自己的信息和子样本列表
 */
@Data
public class SampleTraceVO {
    private String sampleId;
    private String sampleName;
    private String sampleType;
    private String deriveType;       // ORIGINAL / SPLIT / MERGE
    private Integer generation;       // 谱系代数
    private String status;
    private BigDecimal volumeUl;
    private String containerCode;
    private String locationCode;
    private LocalDateTime createTime;
    private List<SampleTraceVO> children;  // 子样本
}
