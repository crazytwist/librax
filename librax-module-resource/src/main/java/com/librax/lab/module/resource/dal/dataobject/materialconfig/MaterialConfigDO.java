package com.librax.lab.module.resource.dal.dataobject.materialconfig;

import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;

/**
 * 物料配置 DO
 *
 * @author 一南
 */
@TableName("res_material_config")
@KeySequence("res_material_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialConfigDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 类型
     */
    private String type;
    /**
     * 类型前缀
     */
    private String typePrefix;
    /**
     * 类型名称
     */
    private String typeName;
    /**
     * ID生成规则
     */
    private String idPattern;
    /**
     * 下一个序列号
     */
    private Integer nextSequence;
    /**
     * 备注
     */
    private String remark;
    /**
     * 额外字段
     */
    private String extData;


}