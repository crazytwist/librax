package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.dal.mysql.slotinfo.SlotInfoMapper;
import com.librax.lab.module.resource.service.agvload.AgvLoadPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AGV 搬运流程中的容量预留和标准报文组装执行器。 */
@Component("agvTransferStepExecutor")
@RequiredArgsConstructor
public class AgvTransferStepExecutor implements StepExecutor {

    private final SlotInfoMapper slotMapper;
    private final AgvLoadPlanService agvLoadPlanService;

    @Override
    public StepTypeEnum supportType() {
        return null; // 仅按 beanName 路由
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StepResult execute(StepDispatchContext ctx) {
        return switch (ctx.getStepKey()) {
            case "agv_reserve_transfer_capacity" -> reserve(ctx);
            case "agv_build_standard_command" -> buildCommand(ctx);
            case "agv_optional_site_action" -> optionalSiteAction(ctx);
            default -> StepResult.fail("AGV_TRANSFER_STEP_UNSUPPORTED",
                    "不支持的AGV搬运步骤: " + ctx.getStepKey());
        };
    }

    private StepResult reserve(StepDispatchContext ctx) {
        List<Map<String, Object>> items = items(ctx.getInputParams().get("transferItems"));
        if (items.isEmpty()) {
            return StepResult.fail("TRANSFER_ITEMS_EMPTY", "transferItems不能为空");
        }

        List<String> reserved = new ArrayList<>();
        try {
            for (Map<String, Object> item : items) {
                String source = required(item, "sourceSlotId");
                String agv = required(item, "agvSlotId");
                String target = required(item, "targetSlotId");
                SlotInfoDO sourceSlot = slotMapper.selectBySlotId(source);
                if (sourceSlot == null) throw new IllegalStateException("源库位不存在: " + source);
                if (slotMapper.reserveIfEmpty(agv) == 0) {
                    throw new IllegalStateException("AGV中转位不足或已占用: " + agv);
                }
                reserved.add(agv);
                if (slotMapper.reserveIfEmpty(target) == 0) {
                    throw new IllegalStateException("目标库位不足或已占用: " + target);
                }
                reserved.add(target);
            }
        } catch (Exception e) {
            reserved.forEach(slotMapper::clearOccupancy);
            return StepResult.fail("TRANSFER_CAPACITY_NOT_ENOUGH", e.getMessage());
        }

        // 多轮模式下，第一波由 startTask 执行，后续波次由 waitSignal + slot/ready 协调。
        // 注册失败直接抛出，使当前事务整体回滚，避免留下孤立预留。
        agvLoadPlanService.registerIfEnabled(ctx.getExecutionId(), items, ctx.getInputParams());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("taskId", ctx.getExecutionId());
        out.put("transferItems", items);
        out.put("reservedSlotIds", reserved);
        out.put("allocatedQuantity", items.size());
        return StepResult.ok(out);
    }

    private StepResult buildCommand(StepDispatchContext ctx) {
        List<Map<String, Object>> items = items(ctx.getInputParams().get("transferItems"));
        List<Map<String, Object>> commands = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("taskType", value(item, "taskType", value(ctx.getInputParams(), "taskType", "1")));
            command.put("plateType", value(item, "plateType", value(ctx.getInputParams(), "plateType", "Plate_5")));
            command.put("step1", requiredMap(item, "step1"));
            command.put("step2", requiredMap(item, "step2"));
            command.put("step3", requiredMap(item, "step3"));
            commands.add(command);
        }
        return StepResult.ok(Map.of(
                "taskId", ctx.getExecutionId(),
                "transferItems", items,
                "agvCmdList", commands));
    }

    private StepResult optionalSiteAction(StepDispatchContext ctx) {
        if (!bool(ctx.getInputParams().get("siteActionEnabled"))) {
            return StepResult.ok(Map.of("siteActionSkipped", true));
        }
        return StepResult.waitForApproval(Map.of(
                "waitReason", value(ctx.getInputParams(), "siteActionPrompt", "请完成AGV现场动作后确认"),
                "assignee", value(ctx.getInputParams(), "siteActionAssignee", "admin")));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance)
                .map(v -> (Map<String, Object>) v).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requiredMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Map)) throw new IllegalArgumentException(key + "不能为空");
        return (Map<String, Object>) value;
    }

    private String required(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException(key + "不能为空");
        return value.toString();
    }

    private Object value(Map<String, Object> map, String key, Object defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    private boolean bool(Object value) {
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value));
    }
}
