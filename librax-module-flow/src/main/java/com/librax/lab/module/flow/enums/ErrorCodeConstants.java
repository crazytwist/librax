package com.librax.lab.module.flow.enums;

import com.librax.lab.framework.common.exception.ErrorCode;

/**
 * Resource 错误码枚举类
 * <p>
 * flow 系统，使用 1_040_001_000 段
 */
public interface ErrorCodeConstants {


    // ── 流程定义 ──────────────────────────────────────────────────────
    ErrorCode PIPELINE_DEFINITION_NOT_EXISTS =
            new ErrorCode(1_040_001_000, "流程定义不存在: pipeline_key={}, version={}");

    ErrorCode PIPELINE_DEFINITION_NOT_ACTIVE =
            new ErrorCode(1_040_001_001, "流程定义未发布: pipeline_key={}, version={}, 当前状态={}");

    ErrorCode PIPELINE_STEP_EMPTY =
            new ErrorCode(1_040_001_002, "流程没有任何节点: pipeline_key={}, version={}");

    // ── 步骤定义 ──────────────────────────────────────────────────────
    ErrorCode STEP_DEFINITION_NOT_EXISTS =
            new ErrorCode(1_040_002_000, "步骤定义不存在: step_key={}");

    ErrorCode STEP_DEFINITION_VERSION_NOT_EXISTS =
            new ErrorCode(1_040_002_001, "步骤定义指定版本不存在: step_key={}, version={}");

    ErrorCode STEP_DEFINITION_NOT_ACTIVE =
            new ErrorCode(1_040_002_002, "步骤定义已停用: step_key={}");

    // ── 流程步骤定义 ──────────────────────────────────────────────────────
    ErrorCode PIPELINE_STEP_NOT_EXISTS =
            new ErrorCode(1_040_010_000, "流程步骤定义不存在: pipeline_step_key={}");

    // ── 流程校验 ──────────────────────────────────────────────────────
    ErrorCode PIPELINE_GRAPH_VALIDATE_FAIL =
            new ErrorCode(1_040_003_000, "流程定义校验失败: {}");

    ErrorCode PIPELINE_GRAPH_CYCLE_DETECTED =
            new ErrorCode(1_040_003_001, "流程定义存在循环依赖，涉及节点: {}");

    ErrorCode PIPELINE_GRAPH_NO_ENTRY_NODE =
            new ErrorCode(1_040_003_002, "流程定义没有入口节点（所有节点都有 dependsOn）");

    ErrorCode PIPELINE_GRAPH_DEPENDS_ON_NOT_EXISTS =
            new ErrorCode(1_040_003_003, "节点 [{}] 的 dependsOn 引用了不存在的节点 [{}]");

    ErrorCode PIPELINE_GRAPH_CONDITION_BRANCH_NOT_EXISTS =
            new ErrorCode(1_040_003_004, "CONDITION 节点 [{}] 的 {} 分支 [{}] 不存在");

    ErrorCode PIPELINE_GRAPH_CONDITION_MISSING_EXPR =
            new ErrorCode(1_040_003_005, "CONDITION 节点 [{}] 缺少 condition_expr");

    ErrorCode PIPELINE_GRAPH_BRANCH_MISSING_DEPENDS =
            new ErrorCode(1_040_003_006, "CONDITION 节点 [{}] 的 {} 分支 [{}] 的 dependsOn 中未包含该 CONDITION 节点");

    ErrorCode PIPELINE_GRAPH_INSTRUMENT_MISSING_DEVICE =
            new ErrorCode(1_040_003_007, "INSTRUMENT 节点 [{}] 缺少 device_type");

    ErrorCode PIPELINE_GRAPH_COMPUTE_MISSING_EXECUTOR =
            new ErrorCode(1_040_003_008, "COMPUTE 节点 [{}] 缺少 executor 配置");

    // ── 流程执行 ──────────────────────────────────────────────────────
    ErrorCode PIPELINE_EXECUTION_NOT_EXISTS =
            new ErrorCode(1_040_004_000, "流程执行实例不存在: execution_id={}");

    ErrorCode PIPELINE_EXECUTION_STATUS_INVALID =
            new ErrorCode(1_040_004_001, "流程执行状态流转非法: {} -> {}");

    ErrorCode PIPELINE_EXECUTION_ZONE_FULL =
            new ErrorCode(1_040_004_002, "区域 [{}] 并发槽位已满，请稍后重试");

    // ── 步骤执行 ──────────────────────────────────────────────────────
    ErrorCode STEP_EXECUTION_NOT_EXISTS =
            new ErrorCode(1_040_005_000, "步骤执行记录不存在: execution_id={}, node_id={}");

    ErrorCode STEP_EXECUTION_STATUS_INVALID =
            new ErrorCode(1_040_005_001, "步骤执行状态流转非法: {} -> {}");

    ErrorCode STEP_NOT_RUNNABLE_STANDALONE =
            new ErrorCode(1_040_005_002, "节点 [{}] 不支持单独运行");


    // ── 流程触发 ──────────────────────────────────────────────────────
    ErrorCode PIPELINE_TRIGGER_NOT_EXISTS =
            new ErrorCode(1_040_100_000, "流程触发定义不存在: step_key={}");


    // ── 流程执行中 ──────────────────────────────────────────────────────
    ErrorCode EXECUTION_CONTEXT_NOT_EXISTS =
            new ErrorCode(1_040_101_000, "执行上下文持久化不存在: step_key={}");
    ErrorCode EXECUTION_EVENT_LOG_NOT_EXISTS =
            new ErrorCode(1_040_101_000, "流程触发定义不存在: step_key={}");

}

