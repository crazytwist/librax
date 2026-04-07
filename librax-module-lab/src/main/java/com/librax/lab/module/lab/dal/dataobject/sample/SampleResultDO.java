package com.librax.lab.module.lab.dal.dataobject.sample;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_] DO
 *
 * @author 一南
 */
@TableName("lab_sample_result")
@KeySequence("lab_sample_result_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleResultDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 结果业务ID
     */
    private String resultId;
    /**
     * 样本ID
     */
    private String sampleId;
    /**
     * 根样本ID，冗余存便于溯源统计
     */
    private String rootSampleId;
    /**
     * 批次号，冗余存便于批次统计
     */
    private String batchNo;
    /**
     * 产生该结果的流程执行ID
     */
    private String executionId;
    /**
     * 产生该结果的步骤节点ID
     */
    private String nodeId;
    /**
     * 检测项目代码：PH/TURBIDITY/COD/BOD/WBC/HGB等
     */
    private String testItem;
    /**
     * 检测项目名称：酸碱度/浊度/化学需氧量等
     */
    private String testItemName;
    /**
     * 检测方法/标准
     */
    private String testMethod;
    /**
     * 检测结果数值（定量结果）
     */
    private BigDecimal resultValue;
    /**
     * 非数值结果（定性结果）：阳性/阴性/未检出/+++等
     */
    private String resultText;
    /**
     * 单位：mg/L、NTU、pH、×10^9/L等
     */
    private String unit;
    /**
     * 结果精度（小数位数），用于显示
     */
    private Integer precisionDigits;
    /**
     * 参考范围下限
     */
    private BigDecimal referenceLow;
    /**
     * 参考范围上限
     */
    private BigDecimal referenceHigh;
    /**
     * 参考范围描述（如"阴性"、"6.5-8.5"）
     */
    private String referenceText;
    /**
     * 是否异常（超出参考范围）
     */
    private Boolean isAbnormal;
    /**
     * 异常标记：HIGH偏高 / LOW偏低 / CRITICAL危急值 / POSITIVE阳性
     */
    private String abnormalFlag;
    /**
     * 检测设备ID
     */
    private String deviceId;
    /**
     * 检测设备名称，冗余存便于展示
     */
    private String deviceName;
    /**
     * 实际检测时间（设备上报时间）
     */
    private LocalDateTime measuredAt;
    /**
     * 设备返回的原始报文/数据
     */
    private String rawData;
    /**
     * 设备原始值（未经换算的）
     */
    private String rawValue;
    /**
     * PENDING待审核 / AUTO_APPROVED自动通过 / APPROVED人工通过 / REJECTED驳回 / RETEST需复测
     */
    private String reviewStatus;
    /**
     * 审核人
     */
    private String reviewedBy;
    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;
    /**
     * 审核意见
     */
    private String reviewComment;
    /**
     * 复测流程执行ID，RETEST时关联
     */
    private String retestExecutionId;
    /**
     * 复测结果ID，关联新的结果记录
     */
    private String retestResultId;
    /**
     * 是否最终结果，复测后原结果标记为0
     */
    private Boolean isFinal;
    /**
     * 备注
     */
    private String remark;


}