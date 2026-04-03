package com.librax.lab.module.flow.dal.dataobject.pipelinetrigger;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 流程触发配置表，管理定时和事件触发规则 [pd_] DO
 *
 * @author 一南
 */
@TableName("pd_pipeline_trigger")
@KeySequence("pd_pipeline_trigger_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTriggerDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 触发器唯一业务 ID，UUID
     */
    private String triggerId;
    /**
     * 关联 pd_pipeline_definition.pipeline_key
     */
    private String pipelineKey;
    /**
     * 指定版本，NULL 表示始终用最新 ACTIVE 版本
     */
    private Integer pipelineVersion;
    /**
     * CRON 定时  EVENT 事件
     */
    private String triggerType;
    /**
     * trigger_type为CRON 时填写，如 0 0 8 * * ?
     */
    private String cronExpr;
    /**
     * trigger_type为EVENT 时填写，MQ topic 名称
     */
    private String eventTopic;
    /**
     * 每次触发时注入的固定 input_params
     */
    private String fixedParams;
    /**
     * 触发时指定执行区域
     */
    private String zoneCode;
    /**
     * ACTIVE 启用 | PAUSED 暂停
     */
    private String status;


}