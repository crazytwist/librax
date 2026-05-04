package com.librax.lab.module.lab.dal.dataobject.materialinstance;

import lombok.*;

import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理 DO
 *
 * @author 芋道源码
 */
@TableName("lab_material_instance")
@KeySequence("lab_material_instance_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialInstanceDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 实例唯一ID，UUID格式，如 INST-20260501-0001
     */
    private String instanceId;
    /**
     * 容器类型编码，关联 lab_container_type.type_code，如 PLATE_96_WELL / TUBE_EP_15ML
     */
    private String typeCode;
    /**
     * 条形码/二维码，扫码追踪用，同一系统内唯一
     */
    private String barcode;
    /**
     * 父实例ID，关联同表 instance_id。试管在试管架里时填试管架的instance_id，孔在孔板里填孔板的instance_id，顶层容器为NULL
     */
    private String parentId;
    /**
     * 在父容器中的位置索引，如孔板孔位 A1/B3，试管架位置 01/02，父容器为NULL时此字段也为NULL
     */
    private String slotIndex;
    /**
     * 当前所在库位ID，关联 lab_slot_info.slot_id。顶层容器才有值，子单元（孔）位置跟随父容器，此字段为NULL
     */
    private String slotId;
    /**
     * 当前所在区域，冗余存储便于按区查询，跟随slot_id所在区域
     */
    private String zoneCode;
    /**
     * 内容物类型：REAGENT/STANDARD/BUFFER/SAMPLE/WASTE/EMPTY。EMPTY表示空容器，CARRIER类型容器此字段为NULL
     */
    private String contentType;
    /**
     * 内容物编码，关联 lab_material_def.material_code，EMPTY或CARRIER类型为NULL
     */
    private String materialCode;
    /**
     * 批次号，试剂溯源用，同一批次的试剂 batch_no 相同
     */
    private String batchNo;
    /**
     * 厂商批号（Lot Number），与 batch_no 区分：batch_no是内部入库批次，lot_no是厂商原始批号
     */
    private String lotNo;
    /**
     * 当前体积（微升），液体类内容物填写，固体/空容器为NULL，步骤消耗后更新
     */
    private BigDecimal currentVolUl;
    /**
     * 当前浓度描述，覆盖 material_def 的标准浓度（如稀释后填写实际浓度）
     */
    private String concentration;
    /**
     * 实例状态：AVAILABLE=可用 RESERVED=已预留（步骤申请但未取用） IN_USE=使用中 USED=已使用完 EXPIRED=已过期 DISCARDED=已废弃
     */
    private String status;
    /**
     * 入库日期，保质期从此日期计算
     */
    private LocalDate receivedAt;
    /**
     * 开封日期，开封后有效期从此日期计算，未开封为NULL
     */
    private LocalDate openedAt;
    /**
     * 过期日期，由入库日期+保质期天数计算，到期自动标记EXPIRED
     */
    private LocalDate expiredAt;
    /**
     * 来源流程执行ID，样本类型填写，追踪是哪次实验产生的
     */
    private String sourceExecutionId;
    /**
     * 来源步骤节点ID，配合 source_execution_id 精确追踪
     */
    private String sourceNodeId;
    /**
     * 备注说明
     */
    private String remark;


}