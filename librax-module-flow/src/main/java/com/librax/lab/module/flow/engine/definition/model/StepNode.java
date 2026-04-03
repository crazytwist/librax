package com.librax.lab.module.flow.engine.definition.model;

import com.librax.lab.module.flow.enums.FailStrategyEnum;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class StepNode {

    // ---- 节点身份 ----
    /** 节点 ID，流程内唯一，来自 pd_pipeline_step.node_id */
    private String nodeId;
    /** 步骤标识，来自 pd_step_definition.step_key */
    private String stepKey;
    /** 显示名称 */
    private String name;
    /** 步骤类型 */
    private StepTypeEnum stepType;

    // ---- 编排关系 ----
    /** 前置节点 nodeId 列表，空表示入口节点 */
    private List<String> dependsOn;
    /** CONDITION：true 分支跳转的 nodeId */
    private String trueBranch;
    /** CONDITION：false 分支跳转的 nodeId */
    private String falseBranch;
    /** 分支节点关系  key=分支名称, value=目标nodeId */
    private Map<String, String> branches;
    /** CONDITION：判断表达式，如 ${s_calc.score} >= 80 */
    private String conditionExpr;

    // ---- 执行配置（三层合并后的最终值）----
    // 优先级：pd_pipeline_step > pd_step_definition > pd_pipeline_definition
    /** 超时时间(ms) */
    private long timeoutMs;
    /** 最大重试次数 */
    private int maxAttempts;
    /** 重试退避时间(ms) */
    private long backoffMs;
    /** 节点失败策略 */
    private FailStrategyEnum onFailure;

    // ---- 参数（三层合并后的最终值）----
    /** 合并后的执行参数：default_params 被 params_override 覆盖 */
    private Map<String, Object> params;
    /** 输入映射：{"phValue": "${s_ph.ph}"}，执行前从上下文解析 */
    private Map<String, String> inputMapping;
    /** 输出映射：{"ph": "$.result.phValue"}，执行后从返回值提取 */
    private Map<String, String> outputMapping;

    // ---- 补偿配置 ----
    /** 补偿节点 nodeId（引用本流程已有节点）*/
    private String compensateNodeId;
    /** 补偿步骤 stepKey（直接指定，不依赖流程内节点）*/
    private String compensateStepKey;
    /** 补偿入参 */
    private Map<String, Object> compensateParams;
    /** 触发补偿时机：ON_FAIL / ON_TIMEOUT / ALWAYS */
    private String compensateOn;

    // ---- 单独运行 ----
    /** 是否支持单独运行 */
    private boolean runnableStandalone;
    /** Mock 输出，run_mode=MOCK 时直接返回 */
    private Map<String, Object> mockOutput;

    // ---- COMPUTE 专用 ----
    private String executor;    // BEAN / LITEFLOW
    private String beanName;
    private String methodName;
    private String chainId;

    // ---- INSTRUMENT 专用 ----
    private String deviceType;
    private String command;

    /**
     * 获取分支目标节点：优先用 branches，回退到 trueBranch/falseBranch
     *
     * @param branchName 分支名称
     * @return 目标 nodeId，null 表示没匹配到
     */
    public String resolveBranchTarget(String branchName) {
        // 优先使用新的 branches 配置
        if (branches != null && !branches.isEmpty()) {
            String target = branches.get(branchName);
            if (target != null) return target;
            // 没有精确匹配，尝试 default
            return branches.get("default");
        }
        // 回退到老的 trueBranch/falseBranch
        if ("true".equals(branchName)) return trueBranch;
        if ("false".equals(branchName)) return falseBranch;
        return null;
    }

    /**
     * 获取所有分支配置（新老兼容）
     *
     * @return key=分支名称, value=目标nodeId
     */
    public Map<String, String> getAllBranches() {
        if (branches != null && !branches.isEmpty()) {
            return branches;
        }
        // 回退：从 trueBranch/falseBranch 构建
        Map<String, String> fallback = new HashMap<>();
        if (trueBranch != null) fallback.put("true", trueBranch);
        if (falseBranch != null) fallback.put("false", falseBranch);
        return fallback;
    }

}