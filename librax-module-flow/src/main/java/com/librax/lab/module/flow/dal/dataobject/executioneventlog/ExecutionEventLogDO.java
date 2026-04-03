package com.librax.lab.module.flow.dal.dataobject.executioneventlog;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 执行事件日志，只 INSERT 不修改，全链路追踪与审计 DO
 *
 * @author 一南
 */
@TableName("pe_execution_event_log")
@KeySequence("pe_execution_event_log_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEventLogDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 关联的执行实例
     */
    private String executionId;
    /**
     * 节点 ID，流程级事件为 NULL
     */
    private String nodeId;
    /**
     * 第几次尝试，节点事件时填写
     */
    private Integer attempt;
    /**
     * 执行模式，与 pe_step_execution.run_mode 对应
     */
    private String runMode;
    /**
     * PIPELINE_STARTED | PIPELINE_PAUSED | PIPELINE_RESUMED | PIPELINE_SUCCESS | PIPELINE_FAILED | PIPELINE_CANCELLED | STEP_QUEUED | STEP_STARTED | STEP_SUCCESS | STEP_FAILED | STEP_SKIPPED | STEP_DEAD | STEP_RETRY_SCHEDULED | STEP_COMPENSATE_TRIGGERED | STEP_COMPENSATED | STANDALONE_STARTED | STANDALONE_FINISHED
     */
    private String eventType;
    /**
     * 变更前状态
     */
    private String fromStatus;
    /**
     * 变更后状态
     */
    private String toStatus;
    /**
     * 事件附加数据，如错误原因、重试间隔、设备 ID、补偿执行 ID
     */
    private String payload;
    /**
     * 操作人，系统触发记 SYSTEM
     */
    private String operator;
    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt;


}