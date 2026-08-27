package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.resource.service.agvload.AgvLoadPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 仓储统一编排启动 Bean。
 *
 * <p>下发仓储备料请求并创建 AGV 装载计划，然后进入等待，直到
 * {@code AgvLoadPlanServiceImpl.completeOrchestration()} 通过 stepCallbackSpi 唤醒。
 */
@Slf4j
@Component("agvStartWarehouseOrchestrationBean")
@RequiredArgsConstructor
public class AgvStartWarehouseOrchestrationBean implements StepExecutor {

    private final AgvLoadPlanService agvLoadPlanService;

    @Override
    public StepTypeEnum supportType() {
        return null; // 仅按 beanName 路由
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        List<Map<String, Object>> items = items(ctx.getInputParams().get("transferItems"));
        agvLoadPlanService.startWarehouseOrchestration(
                ctx.getExecutionId(), ctx.getNodeId(), items, ctx.getInputParams());
        log.info("[AgvWarehouse] 仓储编排已启动，等待AGV完成 executionId={} nodeId={} count={}",
                ctx.getExecutionId(), ctx.getNodeId(), items.size());
        return StepResult.waitForApproval(Map.of(
                "waitReason", "等待仓储逐件备料及AGV装载、移动、卸载完成",
                "orchestration", "WAREHOUSE_AGV"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance)
                .map(v -> (Map<String, Object>) v).toList();
    }
}
