package com.librax.lab.module.resource.dal.dataobject.resourceconfig;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 资源配置表,运行时锁状态见Redis DO
 *
 * @author 一南
 */
@TableName("lab_resource_config")
@KeySequence("lab_resource_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceConfigDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 资源唯一ID,建议与设备ID一致,如 PH-METER-01、AGV-01
     */
    private String resourceId;
    /**
     * 资源类型,如 PH_METER / AGV / BENCH / TURBIDITY
     */
    private String resourceType;
    /**
     * 归属类型:EXCLUSIVE 独占 / SHARED 共享
     */
    private String ownershipType;
    /**
     * 独占时必填(归属区域),共享时为空
     */
    private String zoneCode;
    /**
     * 最大并发持有数,常规为1(同一时刻只能一个流程用),特殊场景可设大
     */
    private Integer maxConcurrent;
    /**
     * 是否启用,0=禁用不参与调度
     */
    private Boolean enabled;
    /**
     * 备注,如"位于A区角落,物理遮挡"
     */
    private String remark;


}