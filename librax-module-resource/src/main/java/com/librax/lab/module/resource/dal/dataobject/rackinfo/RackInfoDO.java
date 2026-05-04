package com.librax.lab.module.resource.dal.dataobject.rackinfo;

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
 * 货架/台面定义，库位的上级容器，归 resource 模块管理 DO
 *
 * @author 一南
 */
@TableName("lab_rack_info")
@KeySequence("lab_rack_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RackInfoDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 货架唯一编码，如 RACK-A / BENCH-01
     */
    private String rackId;
    /**
     * 货架名称，如 A区样本货架
     */
    private String rackName;
    /**
     * 货架类型：RACK=货架 BENCH=操作台 INCUBATOR=孵育箱 FREEZER=冰箱
     */
    private String rackType;
    /**
     * 所属区域，关联 lab_zone_quota.zone_code
     */
    private String zoneCode;
    /**
     * 货架原点世界坐标 X（mm），AGV导航基准点
     */
    private BigDecimal coordX;
    /**
     * 货架原点世界坐标 Y（mm）
     */
    private BigDecimal coordY;
    /**
     * 货架原点世界坐标 Z（mm）
     */
    private BigDecimal coordZ;
    /**
     * 货架行数
     */
    private Integer rowCount;
    /**
     * 货架列数
     */
    private Integer colCount;
    /**
     * 货架层数
     */
    private Integer layerCount;
    /**
     * 是否启用：1=启用 0=禁用（禁用后下属库位不参与调度）
     */
    private Boolean enabled;
    /**
     * 备注说明
     */
    private String remark;


}