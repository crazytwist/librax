package com.librax.lab.module.flow.engine.execution.scheduler;

/**
 * 调度器相关常量
 */
public final class SchedulerConstants {

    private SchedulerConstants() {}

    // ================================================================
    // 上下文 Key 常量
    // ================================================================

    /**
     * 流程初始参数在上下文中的 Key
     * 节点可通过 ${input.xxx} 引用此处的字段
     */
    public static final String CONTEXT_KEY_INPUT = "input";

    // ================================================================
    // WAITING 状态相关常量
    // ================================================================

    /**
     * 回调令牌 Key（写入步骤输出和上下文）
     */
    public static final String WAITING_KEY_CALLBACK_TOKEN = "_callbackToken";

    /**
     * 等待原因 Key
     */
    public static final String WAITING_KEY_WAITING_FOR = "_waitingFor";

    /**
     * 等待开始时间 Key
     */
    public static final String WAITING_KEY_WAITING_SINCE = "_waitingSince";

    /**
     * WAITING 中间数据在上下文中的 Key 前缀
     * 完整 Key 格式: {nodeId}_waiting
     */
    public static final String WAITING_CONTEXT_KEY_SUFFIX = "_waiting";
}
