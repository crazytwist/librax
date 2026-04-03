package com.librax.lab.module.flow.dal.dataobject.stepdefinition;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 步骤定义表，可复用的步骤组件库 [pd_] DO
 *
 * @author 一南
 */
@TableName("pd_step_definition")
@KeySequence("pd_step_definition_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepDefinitionDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 步骤唯一标识，小写_下划线，如 ph_measure
     */
    private String stepKey;
    /**
     * 版本号，同 key 下从 1 递增
     */
    private Integer version;
    /**
     * 步骤显示名称，如 PH检测
     */
    private String name;
    /**
     * 步骤说明
     */
    private String description;
    /**
     * INSTRUMENT 仪器 | COMPUTE 计算 | CONDITION 条件 | WAIT 等待 | NOTIFY 通知
     */
    private String stepType;
    /**
     * 设备类型，如 PH_METER，step_type=INSTRUMENT 时必填
     */
    private String deviceType;
    /**
     * 默认设备指令，如 MEASURE，可被 pd_pipeline_step 覆盖
     */
    private String command;
    /**
     * BEAN | LITEFLOW，step_type=COMPUTE 时必填
     */
    private String executor;
    /**
     * executor=BEAN 时的 Spring Bean 名称
     */
    private String beanName;
    /**
     * executor=BEAN 时的方法名
     */
    private String methodName;
    /**
     * executor=LITEFLOW 时的 Chain ID
     */
    private String chainId;
    /**
     * 默认参数值，被 pd_pipeline_step.params_override 同名字段覆盖
     */
    private String defaultParams;
    /**
     * 参数 schema，供前端渲染表单和后端入参校验
     */
    private String paramsSchema;
    /**
     * 步骤输出字段声明，如 ["ph","temperature"]，供后续节点引用时提示
     */
    private String outputFields;
    /**
     * 默认超时时间(ms)
     */
    private Long defaultTimeoutMs;
    /**
     * 默认最大重试次数
     */
    private Integer defaultMaxAttempts;
    /**
     * 默认重试退避时间(ms)
     */
    private Long defaultBackoffMs;
    /**
     * 默认补偿步骤的 step_key，失败触发补偿时调用
     */
    private String compensateStepKey;
    /**
     * 补偿步骤的默认入参
     */
    private String compensateParams;
    /**
     * 是否支持单独运行 1 支持 0 不支持
     */
    private Boolean runnableStandalone;
    /**
     * 单独运行或测试时的 Mock 输出，跳过真实执行
     */
    private String mockOutput;
    /**
     * ACTIVE 启用  DISABLED 停用
     */
    private String status;


}