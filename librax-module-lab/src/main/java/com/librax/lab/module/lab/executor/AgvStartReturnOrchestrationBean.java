package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.resource.service.agvload.AgvReturnPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 下料编排启动 Bean。
 *
 * <p>立即下发 AGV LOAD 任务（从站位取料），进入等待；直到
 * {@code AgvReturnPlanServiceImpl.completeOrchestration()} 通过 stepCallbackSpi 唤醒。
 *
 * <p>入参 {@code returnItems} 格式：
 * <pre>
 * [
 *   {
 *     "instanceId": "MAT-001",
 *     "containerType": "TYPE_A",
 *     "sourceSlotId": "R-C-01",
 *     "agvSlotId": "AGV-01-SLOT-1",
 *     "transitSlotId": "R-E-01",
 *     "warehouseTargetLocation": "SHELF-A-1-2",
 *     "step1": { ... },
 *     "step2": { ... },
 *     "step3": { ... }
 *   }
 * ]
 * </pre>
 */
@Slf4j
@Component("agvStartReturnOrchestrationBean")
@RequiredArgsConstructor
public class AgvStartReturnOrchestrationBean implements StepExecutor {

    private final AgvReturnPlanService agvReturnPlanService;

    @Override
    public StepTypeEnum supportType() {
        return null; // 仅按 beanName 路由
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        List<Map<String, Object>> items = items(ctx.getInputParams().get("returnItems"));
        agvReturnPlanService.startReturnOrchestration(
                ctx.getExecutionId(), ctx.getNodeId(), items, ctx.getInputParams());
        log.info("[AgvReturn] 下料编排已启动，等待AGV装载、移动、卸料及仓储入库完成 executionId={} nodeId={} count={}",
                ctx.getExecutionId(), ctx.getNodeId(), items.size());
        return StepResult.waitForApproval(Map.of(
                "waitReason", "等待AGV取料移动、按波次卸料至中转位、仓储机械臂逐件入库完成",
                "orchestration", "AGV_WAREHOUSE_RETURN"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance)
                .map(v -> (Map<String, Object>) v).toList();
    }
}
