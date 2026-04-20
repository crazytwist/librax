
package com.librax.lab.module.flow.engine.execution.executor;

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
 * Mock 步骤执行器
 *
 * <p>职责：在真实设备/服务未就绪时，模拟执行并返回预设数据，
 * 让流程引擎的主链路可以在没有任何外部依赖的情况下完整跑通。
 *
 * <p>Mock 输出来源（优先级从高到低）：
 * <ol>
 *   <li>{@code pd_pipeline_step.mock_output}  — 当前节点在此流程里的 Mock 输出（最高优先级）
 *   <li>{@code pd_step_definition.mock_output} — 步骤定义的默认 Mock 输出
 *   <li>内置默认值                              — 上面两者都没配置时使用
 * </ol>
 *
 * <p>使用方式：
 * <ul>
 *   <li>在 {@code pd_step_definition.mock_output} 里配置 JSON，如：
 *       {@code {"ph": 7.2, "temperature": 25.1}}
 *   <li>引擎检测到 {@code step_execution.run_mode=MOCK} 时使用此执行器
 *   <li>生产环境不应注册此执行器（通过 Profile 控制）
 * </ul>
 *
 * <p>注意：此执行器会覆盖所有类型（通过 run_mode 判断，不通过 supportType 路由），
 * 因此 {@link #supportType()} 返回 null，工厂不自动注册，由调度器显式调用。
 */
@Slf4j
@Component
public class MockStepExecutor implements StepExecutor {

    @Override
    public StepTypeEnum supportType() {
        return null; // 不参与工厂路由，由调度器显式调用
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        log.info("[MockExecutor] 开始 Mock 执行 executionId={} nodeId={} stepType={}",
                ctx.getExecutionId(), ctx.getNodeId(), ctx.getStepType());

        Map<String, Object> outputs = buildMockOutputs(ctx);

        log.info("[MockExecutor] Mock 执行完成 executionId={} nodeId={} outputs={}",
                ctx.getExecutionId(), ctx.getNodeId(), outputs);

        return StepResult.ok(outputs);
    }

    private Map<String, Object> buildMockOutputs(StepDispatchContext ctx) {
        // 优先使用配置的 mockOutput
        if (ctx.getMockOutput() != null && !ctx.getMockOutput().isEmpty()) {
            return new HashMap<>(ctx.getMockOutput());
        }

        Map<String, Object> defaults = new HashMap<>();
        switch (ctx.getStepType()) {
            case "INSTRUMENT" -> {
                defaults.put("mockValue", 1.0);
                defaults.put("unit",      "N/A");
                defaults.put("deviceId",  ctx.getDeviceType() + "_MOCK");
            }
            case "COMPUTE" -> {
                defaults.put("result", "mock_computed");
                defaults.put("score",  85);
            }
            case "CONDITION" -> {
                defaults.put("conditionResult", true);
                defaults.put("expr", ctx.getConditionExpr());
            }
            case "WAIT"   -> { defaults.put("waited", true);  defaults.put("durationMs", 0); }
            case "NOTIFY" -> {
                defaults.put("notifyId", "MOCK-NOTIFY-" + System.currentTimeMillis());
                defaults.put("sent", true);
            }
            default -> defaults.put("mockExecuted", true);
        }

        log.debug("[MockExecutor] 使用默认 Mock 输出 nodeId={} stepType={}",
                ctx.getNodeId(), ctx.getStepType());
        return defaults;
    }
}
