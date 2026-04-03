package com.librax.lab.module.flow.framework.redis;

public final class RedisKeyConst {

    private RedisKeyConst() {
    }

    // 执行上下文，execution_id 为变量
    // 结构：Hash，field = node_id，value = JSON
    public static final String EXECUTION_CONTEXT = "lab:ctx:%s";

    // 流程图缓存，pipeline_key + version 为变量
    // 结构：String（序列化的 PipelineGraph）
    public static final String PIPELINE_GRAPH = "lab:graph:%s:%d";

    // 调度锁，防止同一 execution_id 并发调度
    // 结构：String（分布式锁）
    public static final String SCHEDULE_LOCK = "lab:schedule:lock:%s";

    // 步骤提交锁，防止同一步骤被重复提交
    // 结构：String（分布式锁）
    public static final String STEP_SUBMIT_LOCK = "lab:step:lock:%s:%s";

    // Zone 槽位计数
    // 结构：String（整数）
    public static final String ZONE_SLOT_COUNT = "lab:zone:count:%s";

    public static String executionContext(String executionId) {
        return String.format(EXECUTION_CONTEXT, executionId);
    }

    public static String pipelineGraph(String pipelineKey, int version) {
        return String.format(PIPELINE_GRAPH, pipelineKey, version);
    }

    public static String scheduleLock(String executionId) {
        return String.format(SCHEDULE_LOCK, executionId);
    }

    public static String stepSubmitLock(String executionId, String nodeId) {
        return String.format(STEP_SUBMIT_LOCK, executionId, nodeId);
    }

    public static String zoneSlotCount(String zoneCode) {
        return String.format(ZONE_SLOT_COUNT, zoneCode);
    }
}