
package com.librax.lab.module.flow.engine.execution.executor;

import com.googlecode.aviator.AviatorEvaluator;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 条件步骤执行器
 *
 * <p>职责：对 {@code StepNode.conditionExpr} 里的表达式进行求值，
 * 返回布尔结果，调度器根据结果决定走 trueBranch 还是 falseBranch。
 *
 * <p>表达式引擎：Aviator（轻量、高性能、支持数学运算和逻辑运算）
 *
 * <p>表达式变量来源：{@code inputParams}，由调度器在调用前从上下文解析。
 * 例如表达式 {@code score >= 80}，inputParams 里需要有 key="score" 的值。
 *
 * <p>输出约定：
 * <ul>
 *   <li>{@code conditionResult=true}  → 调度器走 trueBranch，falseBranch 标记 SKIPPED
 *   <li>{@code conditionResult=false} → 调度器走 falseBranch，trueBranch 标记 SKIPPED
 * </ul>
 *
 * <p>表达式写法示例（在 pd_pipeline_step.condition_expr 里配置）：
 * <pre>
 *   score >= 80
 *   ph > 6.5 && ph < 8.5
 *   ntu <= 5.0
 * </pre>
 * 注意：表达式里的变量名要和 inputMapping 解析后的 key 一致。
 */
@Slf4j
@Component
public class ConditionStepExecutor implements StepExecutor {

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.CONDITION;
    }

    /**
     * 执行条件判断
     *
     * @param node        CONDITION 节点定义（含 conditionExpr、trueBranch、falseBranch）
     * @param executionId 执行实例ID
     * @param inputParams 解析后的入参，包含表达式所需变量
     * @return 成功时 outputs 里含 conditionResult(Boolean)
     *         失败时返回 fail（表达式解析异常）
     */
    @Override
    public StepResult execute(StepNode node,
                              String executionId,
                              Map<String, Object> inputParams) {
        String expr = node.getConditionExpr();
        log.info("[ConditionExecutor] 求值 executionId={} nodeId={} expr={} params={}",
                executionId, node.getNodeId(), expr, inputParams);

        try {
            // Aviator 求值：inputParams 作为变量上下文
            Object evalResult = AviatorEvaluator.execute(expr, inputParams);

            if (!(evalResult instanceof Boolean)) {
                return StepResult.fail("CONDITION_NOT_BOOLEAN",
                        "表达式结果不是布尔类型，expr=" + expr + " result=" + evalResult);
            }

            boolean conditionResult = (Boolean) evalResult;
            log.info("[ConditionExecutor] 求值完成 executionId={} nodeId={} result={}",
                    executionId, node.getNodeId(), conditionResult);

            // 输出 conditionResult，调度器用这个值决定走哪个分支
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("conditionResult", conditionResult);
            outputs.put("expr", expr);
            return StepResult.ok(outputs);

        } catch (Exception e) {
            log.error("[ConditionExecutor] 表达式求值异常 executionId={} nodeId={} expr={} error={}",
                    executionId, node.getNodeId(), expr, e.getMessage());
            return StepResult.fail("CONDITION_EVAL_FAIL",
                    "表达式求值失败: " + expr + "，原因: " + e.getMessage());
        }
    }
}