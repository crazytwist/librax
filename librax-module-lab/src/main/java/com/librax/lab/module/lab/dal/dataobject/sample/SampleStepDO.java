package com.librax.lab.module.lab.dal.dataobject.sample;

import lombok.*;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态 DO
 *
 * @author 一南
 */
@TableName("lab_sample_step")
@KeySequence("lab_sample_step_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleStepDO extends BaseDO {

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
     * 流程执行ID
     */
    private String executionId;
    /**
     * 步骤节点ID
     */
    private String nodeId;
    /**
     * 对应 pe_step_execution.attempt，重试时同步
     */
    private Integer attempt;
    /**
     * 冗余步骤标识，便于按检测类型统计
     */
    private String stepKey;
    /**
     * 冗余步骤类型：INSTRUMENT/COMPUTE等
     */
    private String stepType;
    /**
     * 样本在该步骤中的角色：INPUT待处理 / OUTPUT处理产出 / CONSUMED已消耗
     */
    private String role;
    /**
     * 绑定方式：AUTO自动绑定 / MANUAL人工绑定 / SCAN扫码绑定
     */
    private String bindType;
    /**
     * BOUND已绑定 / PROCESSING处理中 / COMPLETED已完成 / FAILED失败 / UNBOUND已解绑
     */
    private String status;
    /**
     * 处理该样本的设备ID
     */
    private String deviceId;
    /**
     * 设备通道号/端口号
     */
    private String deviceChannel;
    /**
     * 在设备/容器中的位置（如微孔板孔位 A1/B2、样品架位置 1-10）
     */
    private String position;
    /**
     * 该样本在该步骤的原始检测结果（冗余存，快速查看）
     */
    private String resultData;
    /**
     * 处理顺序号，同一步骤内按此排序，NULL表示批量同时处理不区分顺序
     */
    private Integer seqNo;
    /**
     * 绑定时间
     */
    private LocalDateTime boundAt;
    /**
     * 开始处理时间
     */
    private LocalDateTime startedAt;
    /**
     * 完成处理时间
     */
    private LocalDateTime finishedAt;


}