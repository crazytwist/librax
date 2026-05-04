package com.librax.lab.module.lab.dal.dataobject.materialdef;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理 DO
 *
 * @author 芋道源码
 */
@TableName("lab_material_def")
@KeySequence("lab_material_def_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDefDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 内容物唯一编码，如 PH-BUFFER-7 / ETHANOL-75PCT
     */
    private String materialCode;
    /**
     * 内容物名称，如 pH7标准缓冲液 / 75%乙醇
     */
    private String materialName;
    /**
     * 内容物类型：REAGENT=试剂 STANDARD=标准品 BUFFER=缓冲液 SAMPLE=样本 WASTE=废液 MEDIA=培养基 SOLVENT=溶剂
     */
    private String contentType;
    /**
     * 计量单位：ml / ul / mg / g / 个
     */
    private String unit;
    /**
     * 供应商名称
     */
    private String supplier;
    /**
     * 供应商货号/目录号
     */
    private String catalogNo;
    /**
     * CAS号，化学物质标识，危险品管控用
     */
    private String casNo;
    /**
     * 标准浓度描述，如 1mol/L / 75% / pH7.0
     */
    private String concentration;
    /**
     * 存储温度要求，如 2~8℃ / -20℃ / 室温(15~25℃)
     */
    private String storageTemp;
    /**
     * 保质期（天），从入库日期计算，NULL表示不限
     */
    private Integer shelfLifeDays;
    /**
     * 开封后有效期（天），开封后重新计算，NULL表示不限
     */
    private Integer openLifeDays;
    /**
     * 危险品等级：NONE=无危险 LOW=低危 MEDIUM=中危 HIGH=高危 FLAMMABLE=易燃 TOXIC=有毒
     */
    private String hazardLevel;
    /**
     * 其他规格参数 JSON，如 {"purity":"≥99%","grade":"AR"}
     */
    private String specJson;
    /**
     * 是否启用：1=启用 0=停用
     */
    private Boolean enabled;
    /**
     * 备注说明
     */
    private String remark;


}