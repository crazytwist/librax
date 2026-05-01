package com.librax.lab.module.flow.dal.dataobject.stepexecution;

import lombok.*;

import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪 DO
 *
 * @author 一南
 */
@TableName("pe_step_execution")
@KeySequence("pe_step_execution_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionDO extends BaseDO {

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
     * 节点 ID，对应 pd_pipeline_step.node_id
     */
    private String nodeId;
    /**
     * 冗余步骤标识，对应 pd_step_definition.step_key，便于按类型统计
     */
    private String stepKey;
    /**
     * INSTRUMENT | COMPUTE | CONDITION | WAIT | NOTIFY
     */
    private String stepType;
    /**
     * 第几次尝试，1-based，每次重试 INSERT 新行不覆盖历史
     */
    private Integer attempt;
    /**
     * PENDING | RUNNING | SUCCESS | FAILED | SKIPPED | DEAD | COMPENSATING | COMPENSATED
     */
    private String status;
    /**
     * 等待类型：DEVICE_CALLBACK | MANUAL_APPROVE | EXTERNAL_EVENT | TIMER
     */
    private String waitingFor;
    /**
     * 回调令牌，外部回调时必须携带，防止误触发或重放
     */
    private String callbackToken;
    /**
     * NORMAL | STANDALONE | COMPENSATE | MOCK
     */
    private String runMode;
    /**
     * 入参快照，input_mapping 解析后的实际值，执行前打点
     */
    private String inputSnapshot;
    /**
     * 步骤输出，SUCCESS 后写入上下文的字段
     */
    private String outputData;
    /**
     * 错误码，如 DEVICE_TIMEOUT | COMPUTE_ERROR
     */
    private String errorCode;
    /**
     * 错误详情
     */
    private String errorMsg;
    /**
     * 进入 PENDING 时打点
     */
    private LocalDateTime queuedAt;
    /**
     * 进入 RUNNING 时打点
     */
    private LocalDateTime startedAt;
    /**
     * 进入终态时打点
     */
    private LocalDateTime finishedAt;
    /**
     * 排队等待耗时(ms) = started_at - queued_at
     */
    private Long waitMs;
    /**
     * 实际执行耗时(ms) = finished_at - started_at
     */
    private Long executeMs;
    /**
     * 实际分配到的设备 ID，step_type=INSTRUMENT 时填写
     */
    private String deviceId;
    /**
     * 本节点触发的补偿执行 ID，DEAD 且触发补偿时填写
     */
    private String compensateExecutionId;
    /**
     * 执行时所在区域，冗余自步骤定义，便于查询追踪
     */
    private String zoneCode;
    /**
     * 资源是否已申请：0=未申请，1=已持有（宕机恢复时判断是否需要先释放）
     */
    private Integer resourceAcquired;



}