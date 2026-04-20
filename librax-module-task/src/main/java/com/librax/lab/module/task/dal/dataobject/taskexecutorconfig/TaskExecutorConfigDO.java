package com.librax.lab.module.task.dal.dataobject.taskexecutorconfig;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_] DO
 *
 * @author 一南
 */
@TableName("lab_task_executor_config")
@KeySequence("lab_task_executor_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutorConfigDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 执行器类型：INSTRUMENT/AGV/COMPUTE/MANUAL
     */
    private String executorType;
    /**
     * 最大并发任务数
     */
    private Integer maxConcurrent;
    /**
     * 队列容量，超出则拒绝新任务
     */
    private Integer queueCapacity;
    /**
     * 任务默认超时(ms)
     */
    private Long taskTimeoutMs;
    /**
     * 重试退避时间(ms)
     */
    private Long retryBackoffMs;
    /**
     * 是否可用
     */
    private Boolean enabled;
    /**
     * 备注
     */
    private String remark;


}