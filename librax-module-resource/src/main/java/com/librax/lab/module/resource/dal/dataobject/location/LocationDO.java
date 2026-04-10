package com.librax.lab.module.resource.dal.dataobject.location;

import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;
import com.baomidou.mybatisplus.annotation.*;

/**
 * 区位信息 DO
 *
 * @author 一南
 */
@TableName("res_location")
@KeySequence("res_location_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 区域编码
     */
    private String code;
    /**
     * 区域名称
     */
    private String name;
    /**
     * 上级区域
     */
    private Long parentId;
    /**
     * 层级
     */
    private Integer level;
    /**
     * 类型
     */
    private String type;
    /**
     * 坐标
     */
    private String coordinates;
    /**
     * 状态
     */
    private String status;
    /**
     * 用途
     */
    private String purpose;
    /**
     * 备注
     */
    private String remark;
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


}