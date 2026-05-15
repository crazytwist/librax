package com.librax.lab.module.flow.dal.dataobject.pipelineexecution;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 流程执行实例，支持完整流程、节点单独运行、补偿执行 DO
 *
 * @author 一南
 */
@TableName(value = "pe_pipeline_execution", autoResultMap = true)
@KeySequence("pe_pipeline_execution_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineExecutionDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 执行唯一业务 ID，UUID，对外暴露
     */
    private String executionId;
    /**
     * 流程标识，冗余存便于查询
     */
    private String pipelineKey;
    /**
     * 执行时绑定的定义版本，启动后锁定不变
     */
    private Integer pipelineVersion;
    /**
     * PENDING | RUNNING | PAUSED | SUCCESS | FAILED | CANCELLED | COMPENSATING | COMPENSATED
     */
    private String status;
    /**
     * MANUAL | SCHEDULE | EVENT | RETRY | STANDALONE
     */
    private String triggerType;
    /**
     * 触发人 ID 或触发源标识
     */
    private String triggeredBy;
    /**
     * 外部传入初始参数，节点可通过 ${input.xxx} 引用
     */
    private String inputParams;
    /**
     * 父执行 ID，trigger_type=STANDALONE 时填写
     */
    private String parentExecutionId;
    /**
     * 父执行 ID，parentCallbackToken 父级回调认证
     */
    private String parentCallbackToken;
    /**
     * 单独运行的节点 ID，trigger_type=STANDALONE 时填写
     */
    private String standaloneNodeId;
    /**
     * 被补偿的原始执行 ID，补偿执行时填写
     */
    private String originExecutionId;
    /**
     * 首个节点开始执行时填写
     */
    private LocalDateTime startedAt;
    /**
     * 流程进入终态时填写
     */
    private LocalDateTime finishedAt;
    /**
     * 总耗时(ms)
     */
    private Long totalMs;
    /**
     * 关键路径 node_id 列表，流程结束后异步计算写入
     */
    private String criticalPath;
    /**
     * 乐观锁，状态流转时 WHERE row_version=#{v} 防并发
     */
    private Integer rowVersion;
    /**
     * NONE=无样本 OPTIONAL=可选 REQUIRED=必须
     */
    private String sampleMode = "REQUIRED";
    /**
     * 触发样本绑定的节点ID列表
     */
    @TableField(value = "sample_bind_nodes", typeHandler = JacksonTypeHandler.class)
    private List<String> sampleBindNodes;
    /**
     * 待消费的样本ID队列，逗号分隔，延迟绑定模式使用
     */
    private String pendingSampleIds;


}