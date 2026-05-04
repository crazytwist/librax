package com.librax.lab.module.lab.dal.dataobject.containertype;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理 DO
 *
 * @author 芋道源码
 */
@TableName("lab_container_type")
@KeySequence("lab_container_type_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerTypeDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 容器类型唯一编码，如 PLATE_96_WELL / TUBE_EP_15ML / BOTTLE_50ML
     */
    private String typeCode;
    /**
     * 容器类型名称，如 96孔板 / 15ml EP管 / 50ml试剂瓶
     */
    private String typeName;
    /**
     * 容器大类：PLATE=孔板 TUBE=试管/离心管 BOTTLE=瓶 RACK=托盘/架 VIAL=小瓶 TIP=吸头 CHIP=芯片 FILTER=滤膜 BOX=盒
     */
    private String containerType;
    /**
     * 层级角色：CARRIER=载体（承载其他容器，如托盘/孔板） CONTAINER=直接容器（装内容物，如试管/瓶） WELL=孔（孔板的最小单元）
     */
    private String hierarchyRole;
    /**
     * 最大容积（微升），CARRIER 类型为 NULL
     */
    private BigDecimal maxVolUl;
    /**
     * 孔数/位数，仅 CARRIER 类型填写，如 96孔板填 96，24孔试管架填 24
     */
    private Integer wellCount;
    /**
     * 行数，如96孔板=8，24孔架=4，仅 CARRIER 类型
     */
    private Integer wellRows;
    /**
     * 列数，如96孔板=12，24孔架=6，仅 CARRIER 类型
     */
    private Integer wellCols;
    /**
     * 子单元类型编码，CARRIER 类型填写其承载的子容器类型，如孔板→WELL_STANDARD，24孔架→TUBE_EP_15ML
     */
    private String childTypeCode;
    /**
     * 外形尺寸 X（mm），机械臂抓取用
     */
    private BigDecimal sizeXMm;
    /**
     * 外形尺寸 Y（mm）
     */
    private BigDecimal sizeYMm;
    /**
     * 外形尺寸 Z（mm，高度）
     */
    private BigDecimal sizeZMm;
    /**
     * 孔间距（mm），孔板/试管架的相邻位置间距，移液枪多通道操作用
     */
    private BigDecimal wellSpacingMm;
    /**
     * 容器材质，如 PP / PC / GLASS / PS
     */
    private String material;
    /**
     * 其他规格参数 JSON，如 {"color":"transparent","sterile":true}
     */
    private String specJson;
    /**
     * 是否启用：1=启用 0=停用（停用后不能创建该类型实例）
     */
    private Boolean enabled;
    /**
     * 备注说明
     */
    private String remark;


}