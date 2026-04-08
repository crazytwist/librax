package com.librax.lab.module.flow.engine.execution.executor;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * LiteFlow 计算上下文
 *
 * <p>在 ComputeStepExecutor 创建，传入 Chain 执行。
 * Chain 内的组件通过 {@code this.getContextBean(ComputeContext.class)} 获取。
 *
 * <p>使用约定：
 * <ul>
 *   <li>ComputeStepExecutor 创建时写入 inputParams（从流程上下文解析后的入参）
 *   <li>Chain 组件从 inputParams 读取输入
 *   <li>Chain 组件把计算结果写入 outputs
 *   <li>ComputeStepExecutor 执行完毕后从 outputs 提取结果写入流程上下文
 * </ul>
 *
 * <p>示例（在 LiteFlow 组件中使用）：
 * <pre>
 * {@code
 * @LiteflowComponent("scoreCalcNode")
 * public class ScoreCalcNode extends NodeComponent {
 *     @Override
 *     public void process() {
 *         ComputeContext ctx = this.getContextBean(ComputeContext.class);
 *         Map<String, Object> input = ctx.getInputParams();
 *
 *         double ph = ((Number) input.get("phValue")).doubleValue();
 *         int score = ph >= 6.5 && ph <= 8.5 ? 90 : 60;
 *
 *         ctx.putOutput("score", score);
 *         ctx.putOutput("level", score >= 80 ? "PASS" : "FAIL");
 *     }
 * }
 * }
 * </pre>
 */
@Data
public class ComputeContext {

    /**
     * 输入参数（由 ComputeStepExecutor 写入，Chain 组件只读）
     */
    private Map<String, Object> inputParams = new HashMap<>();

    /**
     * 输出结果（由 Chain 组件写入，ComputeStepExecutor 读取）
     */
    private Map<String, Object> outputs = new HashMap<>();

    /**
     * 便捷方法：写入单个输出字段
     */
    public void putOutput(String key, Object value) {
        outputs.put(key, value);
    }

    /**
     * 便捷方法：批量写入输出字段
     */
    public void putAllOutputs(Map<String, Object> data) {
        if (data != null) {
            outputs.putAll(data);
        }
    }

    /**
     * 便捷方法：从输入中取值
     */
    @SuppressWarnings("unchecked")
    public <T> T getInput(String key) {
        return (T) inputParams.get(key);
    }
}