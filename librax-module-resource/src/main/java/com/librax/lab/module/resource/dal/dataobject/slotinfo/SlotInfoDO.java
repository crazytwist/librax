package com.librax.lab.module.resource.dal.dataobject.slotinfo;

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
 * 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理 DO
 *
 * @author 一南
 */
@TableName("lab_slot_info")
@KeySequence("lab_slot_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotInfoDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 库位唯一编码，同时作为 lab_resource_config.resource_id。固定库位如 RACK-A-01，AGV库位如 AGV-01-SLOT-1
     */
    private String slotId;
    /**
     * 库位显示名称，如 A区货架第1槽
     */
    private String slotName;
    /**
     * 库位类型：FIXED=固定台面库位（有世界坐标） AGV=AGV载台槽位（无固定坐标） DEVICE=设备内部位置
     */
    private String slotType;
    /**
     * 用途限制：ANY=不限 SAMPLE=样本 REAGENT=试剂 WASTE=废液 BUFFER=缓冲液 TIP=吸头
     */
    private String slotUsage;
    /**
     * 所属区域，FIXED和DEVICE类型必填，AGV类型为NULL（跟随AGV移动）
     */
    private String zoneCode;
    /**
     * 所属货架编码，关联 lab_rack_info.rack_id，仅 FIXED 类型填写
     */
    private String rackId;
    /**
     * 货架行号（从1开始），便于人工识别位置，仅 FIXED 类型
     */
    private Integer positionRow;
    /**
     * 货架列号（从1开始），仅 FIXED 类型
     */
    private Integer positionCol;
    /**
     * 货架层号（从1开始），单层货架填1，仅 FIXED 类型
     */
    private Integer positionLayer;
    /**
     * 库位世界坐标 X（mm），AGV导航和机械臂定位用，仅 FIXED 类型
     */
    private BigDecimal coordX;
    /**
     * 库位世界坐标 Y（mm），仅 FIXED 类型
     */
    private BigDecimal coordY;
    /**
     * 库位世界坐标 Z（mm），仅 FIXED 类型
     */
    private BigDecimal coordZ;
    /**
     * 宿主设备ID，AGV类型填 AGV-01，DEVICE类型填 CENTRIFUGE-01 等，关联 lab_resource_config.resource_id
     */
    private String ownerId;
    /**
     * 在宿主设备上的位置编号，AGV类型如 SLOT-1，离心机类型如 POS-1，设备驱动用此字段下发指令
     */
    private String localIndex;
    /**
     * 最大容纳数量，一般为1（一个库位放一个容器），特殊场景可设大
     */
    private Integer capacity;
    /**
     * 是否启用：1=启用参与调度 0=禁用（维修/封存时禁用）
     */
    private Boolean enabled;
    /**
     * 备注，如 靠近B区角落物理遮挡
     */
    private String remark;


}