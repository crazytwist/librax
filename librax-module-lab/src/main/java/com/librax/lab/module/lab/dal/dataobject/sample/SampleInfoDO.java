package com.librax.lab.module.lab.dal.dataobject.sample;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联 DO
 *
 * @author 芋道源码
 */
@TableName("lab_sample_info")
@KeySequence("lab_sample_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleInfoDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 样本唯一业务ID（条码/二维码）
     */
    private String sampleId;
    /**
     * 父样本ID，NULL表示原始样本
     */
    private String parentSampleId;
    /**
     * 根样本ID，冗余存便于查整棵谱系树，原始样本此字段=sample_id
     */
    private String rootSampleId;
    /**
     * 衍生类型：ORIGINAL原始 / SPLIT拆分 / MERGE合并 / ALIQUOT分装 / ADD_REAGENT加试剂
     */
    private String deriveType;
    /**
     * 谱系代数，原始样本=0，每拆分/合并一次+1
     */
    private Integer generation;
    /**
     * 样本类型：BLOOD/URINE/TISSUE/WATER/SOIL/AIR/MIXTURE
     */
    private String sampleType;
    /**
     * 样本名称/描述
     */
    private String sampleName;
    /**
     * 容器类型：TUBE试管/PLATE微孔板/VIAL样品瓶/BAG采样袋
     */
    private String containerType;
    /**
     * 容器条码
     */
    private String containerCode;
    /**
     * 当前体积(微升)
     */
    private BigDecimal volumeUl;
    /**
     * 初始体积(微升)，登记时记录，不再变更
     */
    private BigDecimal initialVolumeUl;
    /**
     * 浓度(mg/mL)，部分样本需要
     */
    private BigDecimal concentration;
    /**
     * 扩展属性，不同类型样本有不同属性，如 {"ph":7.2,"color":"透明"}
     */
    private String attributes;
    /**
     * REGISTERED/LOADED/IN_PROCESS/SPLIT/COMPLETED/ARCHIVED/REJECTED/LOST
     */
    private String status;
    /**
     * 当前物理位置（架位编号/工位编号/冷库编号）
     */
    private String locationCode;
    /**
     * 位置详情（如冷库的具体层架位：R2-S3-P5）
     */
    private String locationDetail;
    /**
     * 当前正在执行的流程ID
     */
    private String currentExecutionId;
    /**
     * 当前所在步骤节点ID
     */
    private String currentNodeId;
    /**
     * 样本批次号，同一批送检的样本共享
     */
    private String batchNo;
    /**
     * 检测单号/委托单号
     */
    private String orderNo;
    /**
     * 优先级 0普通 1加急 2特急，影响资源调度排队顺序
     */
    private Integer priority;
    /**
     * 来源：MANUAL手动/IMPORT导入/LIS系统对接/DEVICE设备采集
     */
    private String source;
    /**
     * 外部系统ID（LIS单号/HIS医嘱号等）
     */
    private String externalId;
    /**
     * 采样时间
     */
    private LocalDateTime collectedAt;
    /**
     * 收样时间（到达实验室）
     */
    private LocalDateTime receivedAt;
    /**
     * 样本有效期
     */
    private LocalDateTime expireTime;
    /**
     * 备注
     */
    private String remark;


}