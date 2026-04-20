package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WaitStepExecutor implements StepExecutor {

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.WAIT;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        log.info("[WaitExecutor] 进入人工等待 nodeId={}", ctx.getNodeId());

        Map<String, Object> inputParams = ctx.getInputParams();
        return StepResult.waitForApproval(Map.of(
                "waitReason", inputParams.getOrDefault("waitReason", "等待人工确认"),
                "assignee",   inputParams.getOrDefault("assignee", "")
        ));
    }
}
