package com.librax.lab.module.lab.dal.dataobject.sample;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_] DO
 *
 * @author 一南
 */
@TableName("lab_sample_event")
@KeySequence("lab_sample_event_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleEventDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 样本ID
     */
    private String sampleId;
    /**
     * 关联流程执行ID，非流程操作为NULL
     */
    private String executionId;
    /**
     * 关联节点ID
     */
    private String nodeId;
    /**
     * 事件类型：REGISTERED/RECEIVED/LOADED/SPLIT/MERGE/ALIQUOT/ADD_REAGENT/TRANSFER/BOUND_TO_STEP/MEASURING/MEASURED/COMPLETED/ARCHIVED/REJECTED/LOST/STATUS_CHANGED
     */
    private String eventType;
    /**
     * 操作前状态
     */
    private String fromStatus;
    /**
     * 操作后状态
     */
    private String toStatus;
    /**
     * 操作前位置
     */
    private String locationFrom;
    /**
     * 操作后位置
     */
    private String locationTo;
    /**
     * 操作前体积
     */
    private BigDecimal volumeBeforeUl;
    /**
     * 操作后体积
     */
    private BigDecimal volumeAfterUl;
    /**
     * 关联设备ID
     */
    private String deviceId;
    /**
     * 操作人或设备ID
     */
    private String operator;
    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt;
    /**
     * 附加信息，如拆分明细、试剂信息、检测参数等
     */
    private String payload;
    /**
     * 事件备注
     */
    private String remark;


}