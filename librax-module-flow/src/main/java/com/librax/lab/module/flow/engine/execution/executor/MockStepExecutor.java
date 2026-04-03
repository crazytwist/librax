
package com.librax.lab.module.flow.engine.execution.executor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    /**
     * Mock 执行器不通过工厂的类型路由，返回 null
     * 调度器在 run_mode=MOCK 时显式调用此执行器
     */
    @Override
    public StepTypeEnum supportType() {
        return null;
    }

    /**
     * 模拟执行，直接返回预设输出
     *
     * <p>执行流程：
     * <ol>
     *   <li>从 {@code StepNode.mockOutput} 取预设输出（Builder已合并pipeline_step和step_definition的mock_output）
     *   <li>若无预设输出，生成包含节点基本信息的默认输出
     *   <li>模拟执行延迟（可选，用于测试超时场景）
     *   <li>返回 SUCCESS 结果
     * </ol>
     *
     * @param node        节点定义（含 mockOutput 字段）
     * @param executionId 执行实例ID（日志追踪用）
     * @param inputParams 运行时入参（Mock时通常忽略）
     * @return 包含预设输出的成功结果
     */
    @Override
    public StepResult execute(StepNode node,
                              String executionId,
                              Map<String, Object> inputParams) {
        log.info("[MockExecutor] 开始 Mock 执行 executionId={} nodeId={} stepType={}",
                executionId, node.getNodeId(), node.getStepType());

        // 取预设 Mock 输出
        Map<String, Object> outputs = buildMockOutputs(node);

        log.info("[MockExecutor] Mock 执行完成 executionId={} nodeId={} outputs={}",
                executionId, node.getNodeId(), outputs);

        return StepResult.ok(outputs);
    }

    /**
     * 构建 Mock 输出数据
     *
     * <p>优先使用节点定义中配置的 {@code mockOutput}，
     * 未配置时根据步骤类型返回合理的默认值。
     *
     * @param node 节点定义
     * @return Mock 输出 Map
     */
    private Map<String, Object> buildMockOutputs(StepNode node) {
        // 优先使用配置的 mockOutput
        if (node.getMockOutput() != null && !node.getMockOutput().isEmpty()) {
            return new HashMap<>(node.getMockOutput());
        }

        // 没有配置 mockOutput 时，根据类型生成默认值
        Map<String, Object> defaults = new HashMap<>();
        switch (node.getStepType()) {
            case INSTRUMENT -> {
                // 仪器节点：根据 output_fields 声明生成默认数值
                defaults.put("mockValue", 1.0);
                defaults.put("unit", "N/A");
                defaults.put("deviceId", node.getDeviceType() + "_MOCK");
            }
            case COMPUTE -> {
                // 计算节点：返回计算成功标志
                defaults.put("result", "mock_computed");
                defaults.put("score", 85);
            }
            case CONDITION -> {
                // CONDITION 节点：固定走 true 分支（便于测试主流程）
                defaults.put("conditionResult", true);
                defaults.put("expr", node.getConditionExpr());
            }
            case WAIT -> {
                defaults.put("waited", true);
                defaults.put("durationMs", 0);
            }
            case NOTIFY -> {
                defaults.put("notifyId", "MOCK-NOTIFY-" + System.currentTimeMillis());
                defaults.put("sent", true);
            }
            case SAMPLE_SPLIT -> {
                // 样本拆分节点：生成两个默认子样本
                defaults.put("childSamples", java.util.List.of(
                        Map.of("label", "A", "sampleId", "MOCK-CHILD-A"),
                        Map.of("label", "B", "sampleId", "MOCK-CHILD-B")
                ));
            }
            default -> defaults.put("mockExecuted", true);
        }

        log.debug("[MockExecutor] 使用默认 Mock 输出 nodeId={} stepType={} outputs={}",
                node.getNodeId(), node.getStepType(), defaults);
        return defaults;
    }
}
