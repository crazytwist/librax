package com.librax.lab.module.flow.api.dispatch;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 步骤分发上下文
 *
 * <p>flow 引擎在步骤就绪时组装此对象，通过 {@link DispatchSpi#dispatch} 传递给具体实现。
 * 实现类（DirectDispatchSpi / QueuedDispatchSpi）从这里取所有执行所需的信息。
 *
 * <p>设计原则：
 * <ul>
 *   <li>只放分发和执行需要的字段，不放引擎内部调度状态（如 dependsOn、outputMapping 等）
 *   <li>各步骤类型专用字段按类型分组，未使用的字段为 null，不影响其他类型
 *   <li>通过 {@link #callback} 通知引擎步骤完成，实现类无需感知 DAG 调度逻辑
 * </ul>
 */
@Data
@Builder
public class StepDispatchContext {

    // ── 步骤身份 ──────────────────────────────────────────────────

    /**
     * 流程执行实例ID
     * 对应 pe_pipeline_execution.execution_id
     */
    private String executionId;

    /**
     * 节点ID
     * 对应 pd_pipeline_step.node_id
     */
    private String nodeId;

    /**
     * 第几次尝试（1-based）
     * 重试时递增，对应 pe_step_execution.attempt
     */
    private int attempt;

    /**
     * 回调令牌
     * 执行完成时必须带回，用于 CallbackDispatcher 校验和反查步骤。
     * 对应 pe_step_execution.callback_token
     */
    private String callbackToken;

    // ── 基础执行配置 ──────────────────────────────────────────────

    /**
     * 步骤类型
     * 取值：INSTRUMENT / COMPUTE / CONDITION / WAIT / NOTIFY / MANUAL
     * 对应 pd_step_definition.step_type
     */
    private String stepType;

    /**
     * 任务执行类型,决定 task 模块用哪个 TaskExecutor
     * INSTRUMENT / AGV / COMPUTE / MANUAL
     * 只在 dispatch_mode=QUEUED 时有意义,DIRECT 路径不看这个字段
     * 为空时由 QueuedDispatchSpi 按 stepType 兜底推断
     */
    private String taskType;

    /**
     * 步骤标识
     * 小写下划线，如 ph_measure，对应 pd_step_definition.step_key
     */
    private String stepKey;

    /**
     * 步骤显示名称
     * 用于日志输出和人工待办标题，如 PH检测
     */
    private String stepName;

    /**
     * 合并后的运行时入参
     * 由 StepSubmitter 在组装 context 时将静态 params 和 inputMapping 解析结果合并，
     * 执行器直接使用，无需再做解析
     */
    private Map<String, Object> inputParams;

    // ── 资源调度 ──────────────────────────────────────────────────

    /**
     * 执行区域编码
     * 影响固定资源（仪器）的查找范围和移动资源（AGV）的配额检查。
     * 对应 pd_pipeline_step.zone_code
     */
    private String zoneCode;

    /**
     * 任务优先级
     * 0=普通 1=加急 2=特急，影响队列路径的排队顺序
     */
    private int priority;

    /**
     * 超时时间（ms）
     * 三层合并后的最终值：pipeline_step > step_def > pipeline_def > 全局兜底
     */
    private long timeoutMs;

    /**
     * 最大重试次数
     * 三层合并后的最终值，TaskTimeoutWatchdog 重试时使用
     */
    private int maxAttempts;

    // ── INSTRUMENT 步骤专用 ───────────────────────────────────────

    /**
     * 设备类型
     * INSTRUMENT 步骤必填，如 PH_METER / TURBIDITY_METER，
     * 用于 ResourcePool 查找本区可用设备和 DeviceGateway 路由驱动
     */
    private String deviceType;

    /**
     * 设备指令代码
     * INSTRUMENT 步骤必填，如 MEASURE / COLLECT，
     * 对应 lab_device_command.command_code
     */
    private String commandCode;

    // ── COMPUTE 步骤专用 ──────────────────────────────────────────

    /**
     * 执行器类型
     * COMPUTE 步骤必填，取值：BEAN / LITEFLOW
     */
    private String executor;

    /**
     * Spring Bean 名称
     * executor=BEAN 时必填，如 waterQualityCalculator
     */
    private String beanName;

    /**
     * Bean 方法名
     * executor=BEAN 时必填，方法签名约定为：
     * {@code Map<String, Object> methodName(Map<String, Object> inputParams)}
     */
    private String methodName;

    /**
     * LiteFlow Chain ID
     * executor=LITEFLOW 时必填，如 score_chain
     */
    private String chainId;

    // ── CONDITION 步骤专用 ────────────────────────────────────────

    /**
     * 条件表达式
     * CONDITION 步骤必填，由 Aviator 引擎求值，如 {@code score >= 80}
     */
    private String conditionExpr;

    /**
     * 分支映射表
     * CONDITION 步骤必填，key=分支名称，value=目标 nodeId。
     * 由 StepSubmitter 在组装时从 StepNode.getAllBranches() 提前解析好放入，
     * 执行器直接使用，不再依赖 StepNode。
     * 示例：{"true": "s_archive", "false": "s_retest_notify", "default": "s_archive"}
     */
    private Map<String, String> allBranches;

    // ── MOCK 专用 ─────────────────────────────────────────────────

    /**
     * Mock 输出数据
     * run_mode=MOCK 时 MockStepExecutor 优先使用此字段，
     * 为 null 时按 stepType 生成默认 Mock 数据。
     * 由 StepSubmitter 从 StepNode.getMockOutput() 取入
     */
    private Map<String, Object> mockOutput;

    // ── 回调 ──────────────────────────────────────────────────────

    /**
     * 步骤完成回调
     *
     * <p>实现类在步骤同步完成（成功或失败）后调用此接口，通知引擎推进 DAG 调度。
     *
     * <p>以下情况<b>不调用</b>此接口：
     * <ul>
     *   <li>INSTRUMENT 步骤返回 WAITING，等设备通过 CallbackDispatcher 回调
     *   <li>QUEUED 路径入队后，等 TaskCallbackDispatcher 在任务完成后回调
     * </ul>
     */
    private DispatchCallback callback;

    // ── 工具方法 ──────────────────────────────────────────────────

    /**
     * 是否是仪器类步骤
     */
    public boolean isInstrument() {
        return "INSTRUMENT".equals(stepType);
    }

    /**
     * 是否是计算类步骤
     */
    public boolean isCompute() {
        return "COMPUTE".equals(stepType);
    }

    /**
     * 是否是条件判断步骤
     */
    public boolean isCondition() {
        return "CONDITION".equals(stepType);
    }

    /**
     * 是否需要申请物理资源
     * 有 deviceType 的步骤（INSTRUMENT/AGV）才需要向 ResourcePool 申请
     */
    public boolean needsResource() {
        return deviceType != null;
    }
}