package com.librax.lab.module.lab.dal.dataobject.materialconsumption;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理 DO
 *
 * @author 芋道源码
 */
@TableName("lab_material_consumption")
@KeySequence("lab_material_consumption_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialConsumptionDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 流程执行ID，关联 pe_pipeline_execution.execution_id
     */
    private String executionId;
    /**
     * 步骤节点ID，关联 pd_pipeline_step.node_id
     */
    private String nodeId;
    /**
     * 第几次重试，与 pe_step_execution.attempt 对应
     */
    private Integer attempt;
    /**
     * 操作的物料实例ID，关联 lab_material_instance.instance_id
     */
    private String instanceId;
    /**
     * 容器类型编码，冗余存储便于统计
     */
    private String typeCode;
    /**
     * 内容物编码，冗余存储便于溯源
     */
    private String materialCode;
    /**
     * 批次号，冗余存储便于批次追踪
     */
    private String batchNo;
    /**
     * 操作类型：CONSUME=消耗（体积减少） TRANSFER=转移（位置变化） PRODUCE=产生（步骤输出新物料） DISCARD=废弃 RESERVE=预留 RELEASE=释放预留
     */
    private String action;
    /**
     * 操作前体积（微升），液体类填写
     */
    private BigDecimal volBeforeUl;
    /**
     * 体积变化量（微升），消耗为负值如-100，产生为正值如+200，转移为0
     */
    private BigDecimal volChangeUl;
    /**
     * 操作后体积（微升），= vol_before_ul + vol_change_ul
     */
    private BigDecimal volAfterUl;
    /**
     * 操作前数量（个），固体/耗材类填写
     */
    private Integer countBefore;
    /**
     * 数量变化量（个），消耗为负值如-2，产生为正值如+5
     */
    private Integer countChange;
    /**
     * 操作后数量（个），= count_before + count_change
     */
    private Integer countAfter;
    /**
     * 转移来源库位，TRANSFER操作时填写
     */
    private String fromSlotId;
    /**
     * 转移目标库位，TRANSFER操作时填写
     */
    private String toSlotId;
    /**
     * 操作发生时间
     */
    private LocalDateTime consumedAt;
    /**
     * 备注说明
     */
    private String remark;


}