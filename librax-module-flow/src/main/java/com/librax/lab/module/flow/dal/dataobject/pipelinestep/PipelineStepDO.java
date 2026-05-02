package com.librax.lab.module.flow.dal.dataobject.pipelinestep;

import lombok.*;

import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_] DO
 *
 * @author 一南
 */
@TableName("pd_pipeline_step")
@KeySequence("pd_pipeline_step_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStepDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 关联 pd_pipeline_definition.pipeline_key
     */
    private String pipelineKey;
    /**
     * 关联 pd_pipeline_definition.version
     */
    private Integer pipelineVersion;
    /**
     * 节点 ID，同一流程版本内唯一，小写+下划线，如 s_ph
     */
    private String nodeId;
    /**
     * DIRECT=直连执行 QUEUED=压入区域队列
     */
    private String dispatchMode;
    /**
     * 任务执行类型,QUEUED 模式下控制 TaskRouter 路由
     */
    private String taskType;
    /**
     * 关联 pd_step_definition.step_key
     */
    private String stepKey;
    /**
     * 指定步骤版本，NULL 表示始终用最新 ACTIVE 版本
     */
    private Integer stepVersion;
    /**
     * 前置节点 node_id 列表，NULL 或空数组表示入口节点，如 ["s_sample"]
     */
    private String dependsOn;
    /**
     * CONDITION 节点判断表达式，如 ${s_calc.score} >= 80
     */
    private String conditionExpr;
    /**
     * CONDITION 节点 expr=true 时跳转的 node_id
     */
    private String trueBranch;
    /**
     * CONDITION 节点多分支配置（N 叉模式），JSON 对象格式。
     * key=匹配值（"*" 表示默认兜底），value=目标 node_id。
     * 非空时优先使用多分支路由，忽略 true_branch/false_branch。
     * 示例：{"GRADE_A":"s_archive_a","GRADE_B":"s_archive_b","*":"s_retest"}
     */
    private String branches;
    /**
     * CONDITION 节点 expr=false 时跳转的 node_id
     */
    private String falseBranch;
    /**
     * 覆盖 pd_step_definition.default_params，只填需要覆盖的字段
     */
    private String paramsOverride;
    /**
     * 从上下文引用前置节点输出，如 {"phValue": "${s_ph.ph}"}
     */
    private String inputMapping;
    /**
     * 将执行返回值映射为语义字段，如 {"ph": "$.result.phValue"}
     */
    private String outputMapping;
    /**
     * 超时时间(ms)，NULL 则向上取默认值
     */
    private Long timeoutMs;
    /**
     * 最大重试次数，NULL 则向上取默认值
     */
    private Integer maxAttempts;
    /**
     * 重试退避时间(ms)，NULL 则向上取默认值
     */
    private Long backoffMs;
    /**
     * FAIL_PIPELINE | SKIP | RETRY_ONLY | COMPENSATE，覆盖流程级 fail_strategy，NULL 则继承
     */
    private String onFailure;
    /**
     * 失败时触发的补偿节点 node_id，引用本流程内已有节点
     */
    private String compensateNodeId;
    /**
     * 失败时触发的补偿步骤 step_key，直接指定不依赖流程内已有节点
     */
    private String compensateStepKey;
    /**
     * 补偿步骤入参，支持 ${node_id.field} 引用上下文
     */
    private String compensateParams;
    /**
     * ON_FAIL | ON_TIMEOUT | ALWAYS，NULL 继承流程级策略
     */
    private String compensateOn;
    /**
     * 默认设备指令，如 MEASURE，可被 pd_pipeline_step 覆盖
     */
    private String command;
    /**
     * 是否支持单独运行，覆盖 pd_step_definition.runnable_standalone
     */
    private Boolean runnableStandalone;
    /**
     * 单独运行或测试时的 Mock 输出，覆盖 pd_step_definition.mock_output
     */
    private String mockOutput;
    /**
     * 前端展示排序，不影响调度逻辑
     */
    private Integer sortOrder;
    /**
     * 画布坐标，如 {"x": 200, "y": 150}，供可视化编辑器使用
     */
    private String uiPosition;

    /**
     * 是否启用资源
     */
    private Boolean resourceEnabled;

    /**
     * 所属区域编码
     */
    private String zoneCode;

    /**
     * 资源等待超时时间(ms)，步骤等待资源超过此值后触发失败，NULL 则用默认值
     */
    private Long resourceWaitTimeoutMs;

}