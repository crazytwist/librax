package com.librax.lab.module.resource.dal.dataobject.zonequota;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 区域对共享资源的配额 DO
 *
 * @author 一南
 */
@TableName("lab_zone_quota")
@KeySequence("lab_zone_quota_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneQuotaDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 区域编码,如 ZONE-A / ZONE-B
     */
    private String zoneCode;
    /**
     * 共享资源类型,如 AGV
     */
    private String resourceType;
    /**
     * 本区最多同时借用数
     */
    private Integer maxBorrow;
    /**
     * 配额说明
     */
    private String remark;


}