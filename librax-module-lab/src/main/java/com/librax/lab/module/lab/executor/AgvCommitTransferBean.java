package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.resource.dal.mysql.slotinfo.SlotInfoMapper;
import com.librax.lab.module.resource.service.agvload.AgvLoadPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * AGV 搬运库位提交 Bean。
 *
 * <p>仓储编排流程完成后，将 AGV 载台上的物料正式写入目标库位，释放中转位占用。
 * 若存在 {@code AgvLoadPlan}（多轮装载），委托 {@link AgvLoadPlanService#commitTransfer} 处理；
 * 否则按 {@code transferItems} 列表逐条提交。
 */
@Slf4j
@Component("agvCommitTransferBean")
@RequiredArgsConstructor
public class AgvCommitTransferBean implements StepExecutor {

    private final AgvLoadPlanService agvLoadPlanService;
    private final SlotInfoMapper slotMapper;

    @Override
    public StepTypeEnum supportType() {
        return null; // 仅按 beanName 路由
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StepResult execute(StepDispatchContext ctx) {
        if (agvLoadPlanService.hasPlan(ctx.getExecutionId())) {
            agvLoadPlanService.commitTransfer(ctx.getExecutionId());
            log.info("[AgvCommit] 多轮装载提交完成 executionId={}", ctx.getExecutionId());
            return StepResult.ok(Map.of("committed", true, "multiLoad", true));
        }
        List<Map<String, Object>> items = items(ctx.getInputParams().get("transferItems"));
        for (Map<String, Object> item : items) {
            String source = required(item, "sourceSlotId");
            String agv = required(item, "agvSlotId");
            String target = required(item, "targetSlotId");
            String instanceId = String.valueOf(item.getOrDefault("instanceId", ""));
            slotMapper.clearOccupancy(source);
            slotMapper.clearOccupancy(agv);
            if (slotMapper.occupyReserved(target, instanceId) == 0) {
                throw new IllegalStateException("目标库位预留已丢失: " + target);
            }
        }
        log.info("[AgvCommit] 搬运库位提交完成 executionId={} count={}", ctx.getExecutionId(), items.size());
        return StepResult.ok(Map.of("committed", true, "transferredQuantity", items.size()));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance)
                .map(v -> (Map<String, Object>) v).toList();
    }

    private String required(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException(key + "不能为空");
        return value.toString();
    }
}
