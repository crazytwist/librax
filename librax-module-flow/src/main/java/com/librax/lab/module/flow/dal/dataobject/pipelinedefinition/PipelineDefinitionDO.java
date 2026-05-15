package com.librax.lab.module.flow.dal.dataobject.pipelinedefinition;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_] DO
 *
 * @author 一南
 */
@TableName(value = "pd_pipeline_definition", autoResultMap = true)
@KeySequence("pd_pipeline_definition_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineDefinitionDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 流程唯一标识，小写+下划线，如 water_quality_test
     */
    private String pipelineKey;
    /**
     * 版本号，同 key 下从 1 递增
     */
    private Integer version;
    /**
     * 流程显示名称
     */
    private String name;
    /**
     * 流程说明
     */
    private String description;
    /**
     * FAIL_FAST 任意节点失败即终止 | CONTINUE_ON_FAIL 跳过失败节点继续执行
     */
    private String failStrategy;
    /**
     * NONE 不补偿 | ON_FAIL 失败时触发 | ALWAYS 无论成败都触发
     */
    private String compensateStrategy;
    /**
     * 流程级默认节点超时(ms)，可被 pd_pipeline_step 覆盖
     */
    private Long defaultTimeoutMs;
    /**
     * 流程级默认最大重试次数，可被 pd_pipeline_step 覆盖
     */
    private Integer defaultMaxAttempts;
    /**
     * 流程级默认退避时间(ms)，可被 pd_pipeline_step 覆盖
     */
    private Long defaultBackoffMs;
    /**
     * DRAFT 草稿 | ACTIVE 已发布 | DISABLED 已停用
     */
    private String status;
    /**
     * 发布时间，status=ACTIVE 时填写
     */
    private LocalDateTime publishedAt;
    /**
     * 样本模式
     */
    private String sampleMode = "REQUIRED";
    /**
     * 触发样本绑定的节点ID列表
     */
    @TableField(value = "sample_bind_nodes", typeHandler = JacksonTypeHandler.class)
    private List<String> sampleBindNodes;
}