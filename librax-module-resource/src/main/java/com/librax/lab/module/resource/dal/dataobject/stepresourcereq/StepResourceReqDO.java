package com.librax.lab.module.resource.dal.dataobject.stepresourcereq;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 步骤资源需求定义，一个步骤节点可配多行（一步多资源） DO
 *
 * @author 芋道源码
 */
@TableName("pd_step_resource_req")
@KeySequence("pd_step_resource_req_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResourceReqDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 关联 pd_pipeline_step.id，步骤节点级别的资源需求
     */
    private Long pipelineStepId;
    /**
     * 需要的资源类型，如 PH_METER、AGV，对应 lab_resource_config.resource_type
     */
    private String resourceType;
    /**
     * 需要数量，通常为1，AGV等可能>1
     */
    private Integer quantity;
    /**
     * 1=独占（同时只能一个步骤用）0=共享（只读设备可并发）
     */
    private Boolean isExclusive;
    /**
     * 全局统一申请顺序，所有步骤必须按相同顺序申请，防死锁
     */
    private Integer sortOrder;


}