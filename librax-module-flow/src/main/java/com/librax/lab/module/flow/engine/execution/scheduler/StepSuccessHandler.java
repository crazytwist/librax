package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.context.ExecutionContextManager;
import com.librax.lab.module.flow.engine.execution.context.OutputMappingResolver;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 步骤成功处理器 — 负责更新状态、应用 output_mapping、释放资源、触发下轮调度
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StepSuccessHandler {

    private final StepStateMachine stepStateMachine;
    private final ExecutionContextManager contextManager;
    private final OutputMappingResolver outputMappingResolver;
    private final StepSubmitter stepSubmitter;

    /**
     * 处理步骤成功
     *
     * @return 是否触发了下一轮调度
     */
    public boolean handle(String executionId,
                          PipelineGraph graph,
                          StepNode node,
                          int attempt,
                          StepResult result,
                          Runnable scheduleTrigger) {
        // 1. 更新步骤状态为 SUCCESS
        stepStateMachine.markSuccess(executionId, node.getNodeId(), attempt, result);

        // 2. 应用 output_mapping，写入上下文
        applyOutputMapping(executionId, node, result);

        // 3. ★ 释放资源（有则释放，无则跳过）
        stepSubmitter.releaseIfHeld(executionId, node.getNodeId(), attempt, "STEP_COMPLETE");

        // 4. 触发下一轮调度
        scheduleTrigger.run();
        return true;
    }

    /**
     * 应用 output_mapping 并写入上下文
     */
    private void applyOutputMapping(String executionId,
                                    StepNode node,
                                    StepResult result) {
        Map<String, Object> outputs = result.getOutputs();
        if (outputs == null || outputs.isEmpty()) {
            return;
        }

        Map<String, String> outputMapping = node.getOutputMapping();
        Map<String, Object> contextOutputs;

        if (outputMapping != null && !outputMapping.isEmpty()) {
            contextOutputs = outputMappingResolver.resolve(outputs, outputMapping);
            log.info("[StepSuccessHandler] output_mapping 应用完成 nodeId={} raw={} mapped={}",
                    node.getNodeId(), outputs.keySet(), contextOutputs.keySet());
        } else {
            contextOutputs = outputs;
        }

        contextManager.putNodeOutput(executionId, node.getNodeId(), contextOutputs);
    }
}