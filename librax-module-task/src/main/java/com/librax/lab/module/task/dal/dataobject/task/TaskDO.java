package com.librax.lab.module.task.dal.dataobject.task;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_] DO
 *
 * @author 一南
 */
@TableName("lab_task")
@KeySequence("lab_task_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 任务唯一业务ID，T-{雪花}
     */
    private String taskId;
    /**
     * 任务类型：INSTRUMENT/AGV/COMPUTE/MANUAL/NOTIFY
     */
    private String taskType;
    /**
     * 任务名称，便于展示，如 PH检测-执行
     */
    private String taskName;
    /**
     * 关联 pe_pipeline_execution.execution_id
     */
    private String executionId;
    /**
     * 关联 pd_pipeline_step.node_id
     */
    private String nodeId;
    /**
     * 冗余步骤标识，便于统计
     */
    private String stepKey;
    /**
     * 关联样本ID，单样本任务填写
     */
    private String sampleId;
    /**
     * 批次号，冗余存便于批次维度统计
     */
    private String batchNo;
    /**
     * 优先级：0普通 1加急 2特急，跨类型统一排序
     */
    private Integer priority;
    /**
     * 期望执行区域，影响执行器选择
     */
    private String zoneCode;
    /**
     * 期望最早执行时间，NULL表示立即
     */
    private LocalDateTime scheduledAt;
    /**
     * 截止时间，超时未完成触发告警
     */
    private LocalDateTime deadlineAt;
    /**
     * 被分配的执行单元ID，如 deviceId/agvId
     */
    private String executorId;
    /**
     * 执行单元类型：DEVICE/AGV/BEAN/HUMAN
     */
    private String executorType;
    /**
     * 外部系统任务ID，如设备侧taskId/AGV调度端jobId
     */
    private String externalTaskId;
    /**
     * PENDING/ASSIGNED/EXECUTING/DONE/FAILED/CANCELLED/TIMEOUT
     */
    private String status;
    /**
     * 失败原因简述
     */
    private String failReason;
    /**
     * 重试次数 对应 pe_step_execution.attempt  步骤的重试次数
     */
    private Integer attempt;
    /**
     * 当前已重试次数
     */
    private Integer retryCount;
    /**
     * 最大重试次数
     */
    private Integer maxRetry;
    /**
     * 回调令牌，执行完成时校验用，与 pe_step_execution 一致
     */
    private String callbackToken;
    /**
     * 执行参数，各任务类型自定义结构
     */
    private String payload;
    /**
     * 执行结果输出，成功后写入，回调时带给引擎
     */
    private String result;
    /**
     * 错误码，失败时填写
     */
    private String errorCode;
    /**
     * 错误详情
     */
    private String errorMsg;
    /**
     * 进入队列时间
     */
    private LocalDateTime queuedAt;
    /**
     * 分配给执行单元时间
     */
    private LocalDateTime assignedAt;
    /**
     * 开始执行时间
     */
    private LocalDateTime startedAt;
    /**
     * 完成时间（终态）
     */
    private LocalDateTime finishedAt;
    /**
     * 排队等待耗时 = assigned_at - queued_at
     */
    private Long waitMs;
    /**
     * 实际执行耗时 = finished_at - started_at
     */
    private Long executeMs;


}