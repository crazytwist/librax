package com.librax.lab.module.lab.dal.dataobject.sample;

import lombok.*;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 样本谱系关系表，记录拆分/合并/分装等衍生关系 DO
 *
 * @author 一南
 */
@TableName("lab_sample_relation")
@KeySequence("lab_sample_relation_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleRelationDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 当前样本ID（衍生出的新样本）
     */
    private String sampleId;
    /**
     * 关联样本ID（来源样本）
     */
    private String relatedSampleId;
    /**
     * 关系类型：SPLIT_FROM拆分来源 / MERGE_FROM合并来源 / ALIQUOT_FROM分装来源 / DERIVED_FROM衍生来源
     */
    private String relationType;
    /**
     * 转移量(微升)，拆分/分装时记录从来源取了多少
     */
    private BigDecimal quantityUl;
    /**
     * 在哪次流程执行中发生的
     */
    private String executionId;
    /**
     * 在哪个步骤中发生的
     */
    private String nodeId;
    /**
     * 备注
     */
    private String remark;


}