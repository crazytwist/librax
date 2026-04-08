package com.librax.lab.module.lab.dal.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 样本拆分结果
 */
@Data
public class SampleSplitResultVO {

    /** 生成的子样本列表 */
    private List<ChildSample> childSamples;

    /** 总拆分体积 */
    private BigDecimal totalSplitVolumeUl;

    @Data
    public static class ChildSample {
        /** 子样本ID */
        private String sampleId;
        /** 标签 */
        private String label;
        /** 分配体积 */
        private BigDecimal volumeUl;
    }
}