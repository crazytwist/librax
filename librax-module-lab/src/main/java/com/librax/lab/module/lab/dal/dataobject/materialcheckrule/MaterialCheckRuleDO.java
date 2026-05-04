package com.librax.lab.module.lab.dal.dataobject.materialcheckrule;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理 DO
 *
 * @author 芋道源码
 */
@TableName("lab_material_check_rule")
@KeySequence("lab_material_check_rule_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCheckRuleDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 关联 pd_pipeline_step.id，步骤节点级别（不是步骤模板级别）
     */
    private Long pipelineStepId;
    /**
     * 需要检查的内容物编码，关联 lab_material_def.material_code
     */
    private String materialCode;
    /**
     * 内容物类型（可选），和 material_code 二选一或组合使用。只填类型时检查该类型任意物料
     */
    private String contentType;
    /**
     * 最低体积要求（微升），液体类填写，不足时阻止执行
     */
    private BigDecimal minVolUl;
    /**
     * 最低数量要求（个），固体/耗材类填写
     */
    private Integer minCount;
    /**
     * 指定从哪个区域取用，NULL表示不限区域
     */
    private String zoneCode;
    /**
     * 检查顺序（同一步骤有多种物料需求时的检查顺序）
     */
    private Integer sortOrder;
    /**
     * 备注说明，如 此步骤消耗约50ul缓冲液
     */
    private String remark;


}