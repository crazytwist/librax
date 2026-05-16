package com.librax.lab.module.resource.dal.dataobject.stepresourcehold;

import lombok.*;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 步骤执行资源占用记录，released_at IS NULL 表示当前持有中 DO
 *
 * @author 一南
 */
@TableName("pe_step_resource_hold")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResourceHoldDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 关联 pe_pipeline_execution.execution_id
     */
    private String executionId;

    /**
     * 关联 pd_pipeline_step.node_id
     */
    private String nodeId;

    /**
     * 第几次重试，与 pe_step_execution.attempt 对应
     */
    private Integer attempt;

    /**
     * 实际占用的具体设备ID，关联 lab_resource_config.resource_id
     */
    private String resourceId;

    /**
     * 资源类型，冗余存便于查询
     */
    private String resourceType;

    /**
     * 申请到资源的时间
     */
    private LocalDateTime acquiredAt;

    /**
     * NULL=当前持有中，有值=已释放
     */
    private LocalDateTime releasedAt;

    /**
     * STEP_COMPLETE / STEP_DEAD / TIMEOUT / CANCELLED / RECOVERY
     */
    private String releaseReason;

    /**
     * 持有类型
     * ACQUIRED  = 当前节点自己竞争申请到的
     * INHERITED = 从父流程继承来的，释放时跳过
     */
    @Builder.Default
    private String holdType = "ACQUIRED";

    /**
     * 继承自哪个父执行ID
     * holdType=INHERITED 时填写，用于追溯
     */
    private String inheritedFrom;
}