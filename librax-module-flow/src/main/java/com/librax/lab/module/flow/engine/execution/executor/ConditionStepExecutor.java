
package com.librax.lab.module.flow.engine.execution.executor;

import com.googlecode.aviator.AviatorEvaluator;
import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 条件步骤执行器
 *
 * <p>职责：对 {@code StepNode.conditionExpr} 里的表达式进行求值，
 * 返回分支名称字符串，调度器根据名称匹配 branches 配置决定走哪条路。
 *
 * <p>支持三种表达式返回值：
 * <ul>
 *   <li>String  → 直接作为分支名，如 "ordinary"、"presale"
 *   <li>Boolean → 转为 "true"/"false"（向后兼容老的二叉分支）
 *   <li>Number  → 转为字符串，如 1 → "1"
 * </ul>
 *
 * <p>表达式写法示例：
 * <pre>
 *   // 老的写法（仍然支持）：返回 boolean，匹配 branches 里的 "true"/"false"
 *   score >= 80
 *
 *   // 新的写法：返回字符串，匹配 branches 里的对应 key
 *   waterType                          → 直接取变量值
 *   score >= 90 ? 'excellent' : score >= 60 ? 'pass' : 'fail'
 * </pre>
 *
 * <p>输出约定：
 * <ul>
 *   <li>{@code branchName}       → 求值结果对应的分支名称
 *   <li>{@code conditionResult}  → 原始求值结果（保留，便于日志和调试）
 *   <li>{@code expr}             → 表达式原文
 * </ul>
 */

@Slf4j
@Component
public class ConditionStepExecutor implements StepExecutor {

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.CONDITION;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        String expr = ctx.getConditionExpr();

        log.info("[ConditionExecutor] 求值 executionId={} nodeId={} expr={} params={}",
                ctx.getExecutionId(), ctx.getNodeId(), expr, ctx.getInputParams());

        try {
            Object evalResult = AviatorEvaluator.execute(expr, ctx.getInputParams());
            String branchName = resolveBranchName(evalResult);

            log.info("[ConditionExecutor] 求值完成 executionId={} nodeId={} " +
                            "rawResult={} branchName={}",
                    ctx.getExecutionId(), ctx.getNodeId(), evalResult, branchName);

            // branches 从 ctx 取（StepDispatchContext 需补充此字段，见下方说明）
            Map<String, String> allBranches = ctx.getAllBranches();
            String targetNodeId = allBranches != null ? allBranches.get(branchName) : null;

            // 尝试 default 兜底
            if (targetNodeId == null && allBranches != null) {
                targetNodeId = allBranches.get("default");
            }

            if (targetNodeId == null) {
                return StepResult.fail("CONDITION_NO_MATCH",
                        String.format("表达式返回 '%s' 但没有匹配的分支，" +
                                        "也没有 default 分支。expr=%s, branches=%s",
                                branchName, expr, allBranches));
            }

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("branchName",      branchName);
            outputs.put("conditionResult", evalResult);
            outputs.put("expr",            expr);
            outputs.put("matchedTarget",   targetNodeId);
            return StepResult.ok(outputs);

        } catch (Exception e) {
            log.error("[ConditionExecutor] 表达式求值异常 executionId={} nodeId={} " +
                            "expr={} error={}",
                    ctx.getExecutionId(), ctx.getNodeId(), expr, e.getMessage());
            return StepResult.fail("CONDITION_EVAL_FAIL",
                    "表达式求值失败: " + expr + "，原因: " + e.getMessage());
        }
    }

    private String resolveBranchName(Object result) {
        if (result == null)             return "default";
        if (result instanceof String s) return s.trim().isEmpty() ? "default" : s.trim();
        if (result instanceof Boolean)  return result.toString();
        return result.toString().trim();
    }
}
