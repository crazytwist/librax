package com.librax.lab.module.task.dal.dataobject.taskevent;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_] DO
 *
 * @author 一南
 */
@TableName("lab_task_event")
@KeySequence("lab_task_event_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEventDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 关联 lab_task.task_id
     */
    private String taskId;
    /**
     * 冗余，便于按类型查日志
     */
    private String taskType;
    /**
     * CREATED/ASSIGNED/STARTED/DONE/FAILED/RETRYING/CANCELLED/TIMEOUT
     */
    private String eventType;
    /**
     * 初始状态
     */
    private String fromStatus;
    /**
     * 结束状态
     */
    private String toStatus;
    /**
     * 操作的执行单元
     */
    private String executorId;
    /**
     * 附加信息，如分配原因、重试次数、错误详情
     */
    private String payload;
    /**
     * 操作人，系统触发记 SYSTEM
     */
    private String operator;
    /**
     * 发生事件
     */
    private LocalDateTime occurredAt;


}