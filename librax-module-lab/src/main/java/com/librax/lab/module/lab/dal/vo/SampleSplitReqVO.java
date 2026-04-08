package com.librax.lab.module.lab.dal.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 样本拆分请求
 */
@Data
public class SampleSplitReqVO {

    /** 父样本ID */
    private String parentSampleId;

    /** 拆分明细 */
    private List<SplitItem> splits;

    /** 子样本容器类型（选填，默认继承父样本） */
    private String containerType;

    /** 流程执行ID（记录谱系关系用） */
    private String executionId;

    /** 节点ID */
    private String nodeId;

    @Data
    public static class SplitItem {
        /** 子样本标签，如 A/B/C */
        private String label;
        /** 分配体积(微升) */
        private BigDecimal volumeUl;
    }
}