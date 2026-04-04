package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WaitStepExecutor implements StepExecutor{

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.WAIT;
    }

    @Override
    public StepResult execute(StepNode node,
                              String executionId,
                              Map<String, Object> inputParams) {
        log.info("[WaitExecutor] 进入人工等待 nodeId={}", node.getNodeId());

        // 直接返回等待审批，不做任何操作
        // 前端展示该步骤为"待审批"状态，审批人通过 /approve 接口推进
        return StepResult.waitForApproval(Map.of(
                "waitReason", node.getParams()
                        .getOrDefault("waitReason", "等待人工确认"),
                "assignee", node.getParams()
                        .getOrDefault("assignee", "")
        ));
    }
}
