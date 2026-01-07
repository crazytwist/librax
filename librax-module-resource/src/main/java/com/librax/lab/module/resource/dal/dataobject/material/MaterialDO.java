package com.librax.lab.module.resource.dal.dataobject.material;

import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;

/**
 * 物料基础信息 DO
 *
 * @author 芋道源码
 */
@TableName("res_material")
@KeySequence("res_material_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 编码
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 类型
     */
    private String type;
    /**
     * 类别
     */
    private String category;
    /**
     * 状态
     */
    private String status;
    /**
     * 区域
     */
    private String area;
    /**
     * 位置
     */
    private String location;
    /**
     * 额外字段
     */
    private String extData;
    /**
     * 拓展字段1
     */
    private String extField1;
    /**
     * 拓展字段2
     */
    private String extField2;
    /**
     * 拓展字段3
     */
    private String extField3;
    /**
     * 拓展字段4
     */
    private String extField4;
    /**
     * 拓展字段5
     */
    private String extField5;
    /**
     * 备注
     */
    private String remark;


}