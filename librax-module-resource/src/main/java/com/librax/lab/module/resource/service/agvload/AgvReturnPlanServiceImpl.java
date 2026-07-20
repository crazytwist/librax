package com.librax.lab.module.resource.service.agvload;

import com.alibaba.fastjson.JSON;
import com.librax.lab.framework.common.exception.ServiceException;
import com.librax.lab.module.device.api.AgvResumeHandler;
import com.librax.lab.module.device.api.AgvTaskStateHandler;
import com.librax.lab.module.device.api.AgvWaitSignalHandler;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.resource.controller.app.slotinfo.vo.WarehouseCallbackReqVO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnItemDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnPlanDO;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvReturnItemMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvReturnPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvTaskQueueMapper;
import com.librax.lab.module.resource.dal.mysql.slotinfo.SlotInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.*;

/**
 * AGV 下料编排实现。
 *
 * <p>与补料（AgvLoadPlanServiceImpl）完全独立，共享 {@link AgvQueueService} 管理同一台 AGV 的任务队列。
 *
 * <p>波次结构：AGV 一次装载全部物料（单个 LOAD 任务，含所有 step1 指令），
 * 移动到仓储区后，按 {@code waveSize}（中转位容量）分批卸料。每批卸料完成后，
 * 仓储机械臂逐件入库（回调 /slot/returned），全部确认后再卸下一批。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgvReturnPlanServiceImpl implements AgvReturnPlanService,
        AgvTaskStateHandler, AgvWaitSignalHandler, AgvResumeHandler {

    private final AgvReturnPlanMapper planMapper;
    private final AgvReturnItemMapper itemMapper;
    private final AgvTaskQueueMapper queueMapper;
    private final SlotInfoMapper slotMapper;
    private final DeviceCommandSpi deviceCommandSpi;
    private final StepCallbackSpi stepCallbackSpi;
    private final AgvQueueService agvQueueService;

    // ── AgvReturnPlanService ─────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReturnOrchestration(String taskId, String nodeId,
                                          List<Map<String, Object>> returnItems,
                                          Map<String, Object> inputParams) {
        if (planMapper.selectByTaskId(taskId) != null) return;
        if (returnItems == null || returnItems.isEmpty()) {
            throw exception(AGV_LOAD_PLAN_INVALID, "returnItems不能为空");
        }
        int waveSize = positiveInt(inputParams.get("transitCapacity"), 2);
        int agvCapacity = positiveInt(inputParams.get("agvCapacity"), 8);
        if (returnItems.size() > agvCapacity) {
            throw exception(AGV_LOAD_PLAN_INVALID,
                    "单车物料数" + returnItems.size() + "超过AGV容量" + agvCapacity);
        }
        int totalWaves = (returnItems.size() + waveSize - 1) / waveSize;

        // 预留 AGV 槽位和中转位
        for (Map<String, Object> item : returnItems) {
            String agvSlot = required(item, "agvSlotId");
            String transit = required(item, "transitSlotId");
            if (slotMapper.reserveIfEmpty(agvSlot) == 0) {
                throw exception(AGV_LOAD_PLAN_INVALID, "AGV槽位不可用: " + agvSlot);
            }
            if (slotMapper.reserveIfEmpty(transit) == 0) {
                throw exception(AGV_LOAD_PLAN_INVALID, "中转位不可用: " + transit);
            }
        }

        AgvReturnPlanDO plan = AgvReturnPlanDO.builder()
                .taskId(taskId)
                .deviceId(string(inputParams.get("agvDeviceId"), "AGV-01"))
                .expectedCount(returnItems.size())
                .returnedCount(0)
                .waveSize(waveSize)
                .currentWave(1)
                .totalWaves(totalWaves)
                .orchestrationNodeId(nodeId)
                .warehouseCallbackUrl(string(inputParams.get("warehouseCallbackUrl"),
                        "http://localhost:48080/app-api/resource/slot/returned"))
                .taskType(string(inputParams.get("taskType"), "1"))
                .plateType(string(inputParams.get("plateType"), "Plate_5"))
                .status("WAIT_AGV_LOAD")
                .build();
        planMapper.insert(plan);

        List<AgvReturnItemDO> items = new ArrayList<>();
        for (int i = 0; i < returnItems.size(); i++) {
            Map<String, Object> item = returnItems.get(i);
            AgvReturnItemDO entity = AgvReturnItemDO.builder()
                    .taskId(taskId)
                    .sequenceNo(i + 1)
                    .waveNo(i / waveSize + 1)
                    .sourceSlotId(required(item, "sourceSlotId"))
                    .agvSlotId(required(item, "agvSlotId"))
                    .transitSlotId(required(item, "transitSlotId"))
                    .warehouseTargetLocation(required(item, "warehouseTargetLocation"))
                    .instanceId(required(item, "instanceId"))
                    .containerType(string(item.get("containerType"), null))
                    .step1Json(JSON.toJSONString(requiredMap(item, "step1")))
                    .step2Json(item.get("step2") instanceof Map<?, ?> ? JSON.toJSONString(item.get("step2")) : null)
                    .step3Json(JSON.toJSONString(requiredMap(item, "step3")))
                    .status("WAIT_LOAD")
                    .build();
            itemMapper.insert(entity);
            items.add(entity);
        }

        sendLoadTask(plan, items);
        log.info("[AgvReturn] 启动下料编排 taskId={} count={} waveSize={} totalWaves={}",
                taskId, returnItems.size(), waveSize, totalWaves);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> markSlotReturned(WarehouseCallbackReqVO req) {
        if (!org.springframework.util.StringUtils.hasText(req.getTransferTaskId())) {
            log.info("[SlotReturned] 独立入库 slotId={} instanceId={} status={}",
                    req.getFromLocation(), req.getMaterialId(), req.getStatus());
            return Map.of("standalone", true, "slotId", req.getFromLocation());
        }

        AgvReturnPlanDO plan = planMapper.selectByTaskIdForUpdate(req.getTransferTaskId());
        if (plan == null) throw exception(AGV_LOAD_PLAN_NOT_EXISTS, req.getTransferTaskId());

        if (!req.isSuccess()) {
            return handleWarehouseReturnFailure(plan, req);
        }

        AgvReturnItemDO item = itemMapper.selectByTransitSlot(req.getTransferTaskId(), req.getFromLocation());
        if (item == null || !"IN_TRANSIT".equals(item.getStatus())) {
            throw exception(SLOT_READY_CONFLICT,
                    "任务" + req.getTransferTaskId() + "没有在" + req.getFromLocation() + "等待入库的物料");
        }

        item.setStatus("RETURNED");
        item.setReturnedAt(req.getEventTime() != null ? req.getEventTime() : LocalDateTime.now());
        itemMapper.updateById(item);
        slotMapper.clearOccupancy(item.getTransitSlotId());

        int wave = item.getWaveNo();

        // 当前波次还有物料等待仓储处理
        if (requestNextWarehouseReturn(plan, wave)) {
            log.info("[AgvReturn] 波次{}继续入库 taskId={} slotId={}", wave, req.getTransferTaskId(), req.getFromLocation());
            return result(plan, item);
        }

        // 波次全部 RETURNED，检查是否还有下一波
        boolean waveComplete = itemMapper.selectByTaskIdAndWave(req.getTransferTaskId(), wave)
                .stream().allMatch(i -> "RETURNED".equals(i.getStatus()) || "COMPLETED".equals(i.getStatus()));
        if (!waveComplete) {
            return result(plan, item); // 有其他 IN_TRANSIT 项的仓储请求已在途
        }

        plan.setReturnedCount(plan.getReturnedCount() + (int) itemMapper.selectByTaskIdAndWave(req.getTransferTaskId(), wave)
                .stream().filter(i -> "RETURNED".equals(i.getStatus())).count());

        if (plan.getCurrentWave() < plan.getTotalWaves()) {
            plan.setCurrentWave(plan.getCurrentWave() + 1);
            sendUnloadWave(plan);
            log.info("[AgvReturn] 波次{}完成，触发下一波卸料 taskId={}", wave, req.getTransferTaskId());
        } else {
            completeOrchestration(plan);
            log.info("[AgvReturn] 全部入库完成，触发流程回调 taskId={}", req.getTransferTaskId());
        }
        return result(plan, item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void commitReturn(String taskId) {
        AgvReturnPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null) throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
        if (!"WAIT_FLOW_COMMIT".equals(plan.getStatus())) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, plan.getStatus());
        }
        List<AgvReturnItemDO> items = itemMapper.selectByTaskId(taskId);
        for (AgvReturnItemDO item : items) {
            if (!"RETURNED".equals(item.getStatus())) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                        "物料" + item.getSequenceNo() + "状态=" + item.getStatus());
            }
            // AGV 槽位在 UNLOAD 完成时已清理，此处再次保护
            slotMapper.clearOccupancy(item.getAgvSlotId());
            item.setStatus("COMPLETED");
            itemMapper.updateById(item);
        }
        plan.setStatus("COMPLETED");
        planMapper.updateById(plan);
    }

    @Override
    public boolean hasPlan(String taskId) {
        return planMapper.selectByTaskId(taskId) != null;
    }

    @Override
    public Map<String, Object> getProgress(String taskId) {
        AgvReturnPlanDO plan = planMapper.selectByTaskId(taskId);
        if (plan == null) throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
        List<Map<String, Object>> items = itemMapper.selectByTaskId(taskId).stream().map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("sequenceNo", item.getSequenceNo());
            value.put("waveNo", item.getWaveNo());
            value.put("sourceSlotId", item.getSourceSlotId());
            value.put("transitSlotId", item.getTransitSlotId());
            value.put("warehouseTargetLocation", item.getWarehouseTargetLocation());
            value.put("instanceId", item.getInstanceId());
            value.put("status", item.getStatus());
            return value;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", plan.getStatus());
        result.put("returnedCount", plan.getReturnedCount());
        result.put("expectedCount", plan.getExpectedCount());
        result.put("currentWave", plan.getCurrentWave());
        result.put("totalWaves", plan.getTotalWaves());
        result.put("items", items);
        return result;
    }

    // ── AgvTaskStateHandler ──────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String agvTaskId, String runState, String agvId) {
        AgvReturnPlanDO existing = planMapper.selectByCurrentAgvTaskId(agvTaskId);
        if (existing == null || existing.getOrchestrationNodeId() == null) return false;
        if ("1".equals(runState)) return true; // 运行中，不做处理

        AgvReturnPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(agvTaskId);
        if (!"2".equals(runState)) {
            plan.setStatus("FAILED");
            planMapper.updateById(plan);
            queueMapper.cancelByAgvTaskId(agvTaskId);
            stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null,
                    false, Map.of("agvTaskId", agvTaskId, "runState", runState),
                    "AGV_TASK_ERROR", "AGV下料子任务失败 runState=" + runState);
            agvQueueService.dispatchNext(plan.getDeviceId());
            return true;
        }

        queueMapper.markDoneByAgvTaskId(agvTaskId);

        switch (plan.getCurrentAgvOperation()) {
            case "LOAD" -> afterLoadComplete(plan);
            case "MOVE" -> sendUnloadWave(plan);    // 开始第一波卸料
            case "UNLOAD" -> {
                afterUnloadComplete(plan);          // 标记 IN_TRANSIT，请求仓储
                agvQueueService.dispatchNext(plan.getDeviceId()); // 让 AGV 可承接其它任务
            }
            default -> throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                    "未知AGV下料操作: " + plan.getCurrentAgvOperation());
        }
        return true;
    }

    // ── AgvWaitSignalHandler ─────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String taskId, String agvStation) {
        AgvReturnPlanDO existing = planMapper.selectByCurrentAgvTaskId(taskId);
        if (existing == null || existing.getOrchestrationNodeId() == null
                || !existing.getStatus().startsWith("WAIT_AGV_")) return false;
        AgvReturnPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(taskId);
        plan.setStatus("WAIT_SITE_ACTION");
        plan.setLastAgvStation(agvStation);
        planMapper.updateById(plan);
        log.info("[AgvReturn] AGV进入现场动作等待 taskId={} operation={} station={}",
                taskId, plan.getCurrentAgvOperation(), agvStation);
        return true;
    }

    // ── AgvResumeHandler ─────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(String agvTaskId) {
        AgvReturnPlanDO existing = planMapper.selectByCurrentAgvTaskId(agvTaskId);
        if (existing == null || !"WAIT_SITE_ACTION".equals(existing.getStatus())) return false;
        AgvReturnPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(agvTaskId);
        deviceCommandSpi.send("AGV", "startAgain",
                Map.of("taskId", agvTaskId, "disableSysFieldInjection", true),
                plan.getTaskId(), "resume_return_" + plan.getCurrentAgvOperation(), null);
        plan.setStatus("WAIT_AGV_" + plan.getCurrentAgvOperation());
        planMapper.updateById(plan);
        log.info("[AgvReturn] 现场动作完成，通知AGV继续 taskId={} operation={}",
                agvTaskId, plan.getCurrentAgvOperation());
        return true;
    }

    // ── 私有方法 ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void sendLoadTask(AgvReturnPlanDO plan, List<AgvReturnItemDO> allItems) {
        String agvTaskId = plan.getTaskId() + "-LOAD";
        List<Map<String, Object>> commands = new ArrayList<>();
        for (AgvReturnItemDO item : allItems) {
            Map<String, Object> cmd = new LinkedHashMap<>();
            cmd.put("taskType", plan.getTaskType());
            cmd.put("plateType", plan.getPlateType());
            cmd.put("step1", JSON.parseObject(item.getStep1Json(), Map.class));
            commands.add(cmd);
            item.setDispatchedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        sendAndEnqueue(plan, agvTaskId, "LOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", commands, "disableSysFieldInjection", true));
    }

    private void afterLoadComplete(AgvReturnPlanDO plan) {
        List<AgvReturnItemDO> allItems = itemMapper.selectByTaskId(plan.getTaskId());
        for (AgvReturnItemDO item : allItems) {
            slotMapper.clearOccupancy(item.getSourceSlotId()); // 站位库位释放
            item.setStatus("ON_AGV");
            item.setLoadedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        sendMoveTask(plan);
    }

    @SuppressWarnings("unchecked")
    private void sendMoveTask(AgvReturnPlanDO plan) {
        AgvReturnItemDO source = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> item.getStep2Json() != null).findFirst()
                .orElseThrow(() -> exception(AGV_LOAD_PLAN_INVALID, "step2不能为空"));
        String agvTaskId = plan.getTaskId() + "-MOVE";
        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("taskType", plan.getTaskType());
        cmd.put("plateType", plan.getPlateType());
        cmd.put("step2", JSON.parseObject(source.getStep2Json(), Map.class));
        sendAndEnqueue(plan, agvTaskId, "MOVE",
                Map.of("taskId", agvTaskId, "agvCmdList", List.of(cmd), "disableSysFieldInjection", true));
    }

    @SuppressWarnings("unchecked")
    private void sendUnloadWave(AgvReturnPlanDO plan) {
        int wave = plan.getCurrentWave();
        List<AgvReturnItemDO> waveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), wave)
                        && "ON_AGV".equals(item.getStatus()))
                .toList();
        if (waveItems.isEmpty()) {
            throw exception(AGV_LOAD_PLAN_INVALID, "第" + wave + "波无待卸料物料");
        }
        String agvTaskId = plan.getTaskId() + "-UNLOAD-" + wave;
        List<Map<String, Object>> commands = new ArrayList<>();
        for (AgvReturnItemDO item : waveItems) {
            Map<String, Object> cmd = new LinkedHashMap<>();
            cmd.put("taskType", plan.getTaskType());
            cmd.put("plateType", plan.getPlateType());
            cmd.put("step3", JSON.parseObject(item.getStep3Json(), Map.class));
            commands.add(cmd);
        }
        sendAndEnqueue(plan, agvTaskId, "UNLOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", commands, "disableSysFieldInjection", true));
    }

    private void afterUnloadComplete(AgvReturnPlanDO plan) {
        int wave = plan.getCurrentWave();
        List<AgvReturnItemDO> waveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), wave)
                        && "ON_AGV".equals(item.getStatus()))
                .toList();
        for (AgvReturnItemDO item : waveItems) {
            slotMapper.clearOccupancy(item.getAgvSlotId()); // AGV 槽位释放
            slotMapper.occupyReserved(item.getTransitSlotId(), item.getInstanceId()); // 中转位标记为已占用
            item.setStatus("IN_TRANSIT");
            item.setTransitAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        plan.setStatus("WAIT_WAREHOUSE");
        plan.setCurrentAgvTaskId(null);
        plan.setCurrentAgvOperation(null);
        planMapper.updateById(plan);
        requestNextWarehouseReturn(plan, wave);
        log.info("[AgvReturn] 波次{}卸料完成，等待仓储入库 taskId={}", wave, plan.getTaskId());
    }

    /**
     * 向仓储请求下一件物料入库。
     *
     * @return true 表示有物料已发出请求（仓储在处理中）；false 表示当前波次无待请求项
     */
    private boolean requestNextWarehouseReturn(AgvReturnPlanDO plan, int waveNo) {
        AgvReturnItemDO next = itemMapper.selectNextPendingReturn(plan.getTaskId(), waveNo);
        if (next == null) return false;

        String subRequestId = plan.getTaskId() + "-W" + waveNo + "-I" + next.getSequenceNo();
        next.setWarehouseRequestId(subRequestId);
        itemMapper.updateById(next);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("requestId", subRequestId);                        // 幂等键，仓储回调时原样带回
        params.put("transferTaskId", plan.getTaskId());               // 流程ID，回调时作为 taskId 返回
        params.put("materialId", next.getInstanceId());               // 物料实例ID，仓储据此确认物料
        params.put("containerType", next.getContainerType());         // 容器类型
        params.put("fromLocation", next.getTransitSlotId());          // 中转位（物料当前在此）
        params.put("toLocation", next.getWarehouseTargetLocation());  // 目标货架位
        params.put("callbackUrl", plan.getWarehouseCallbackUrl());
        params.put("disableSysFieldInjection", true);

        DeviceCommandSpi.Result response = deviceCommandSpi.send("WAREHOUSE", "prepareMaterials", params,
                plan.getTaskId(), "warehouse_return_" + next.getSequenceNo(), null);
        if (response.getResponseBody() != null && !response.getResponseBody().isBlank()) {
            Integer code = JSON.parseObject(response.getResponseBody()).getInteger("code");
            if (code != null && code != 0 && code != 1005) {
                String message = JSON.parseObject(response.getResponseBody()).getString("message");
                if (message == null) message = JSON.parseObject(response.getResponseBody()).getString("msg");
                throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                        "仓储拒绝入库 code=" + code + ", message=" + message);
            }
        }
        log.info("[AgvReturn] 请求仓储入库 taskId={} subRequestId={} from={} to={}",
                plan.getTaskId(), subRequestId, next.getTransitSlotId(), next.getWarehouseTargetLocation());
        return true;
    }

    private void completeOrchestration(AgvReturnPlanDO plan) {
        plan.setStatus("WAIT_FLOW_COMMIT");
        planMapper.updateById(plan);
        CallbackResult callback = stepCallbackSpi.callback(
                plan.getTaskId(), plan.getOrchestrationNodeId(), null, true,
                Map.of("returnedCount", plan.getExpectedCount(), "warehouseReturn", true),
                null, null);
        if (!callback.isSuccess()) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                    "下料编排节点推进失败: " + callback.getErrorMsg());
        }
    }

    private Map<String, Object> handleWarehouseReturnFailure(AgvReturnPlanDO plan, WarehouseCallbackReqVO req) {
        AgvReturnItemDO failed = itemMapper.selectByTransitSlot(req.getTransferTaskId(), req.getFromLocation());
        if (failed == null) throw exception(SLOT_READY_CONFLICT, "找不到对应的下料入库请求");
        failed.setStatus("FAILED");
        itemMapper.updateById(failed);
        plan.setStatus("FAILED");
        planMapper.updateById(plan);
        if (plan.getOrchestrationNodeId() != null) {
            stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null,
                    false, Map.of("slotId", req.getFromLocation()),
                    "WAREHOUSE_" + (req.getCode() != null ? req.getCode() : 1099),
                    req.getMessage() != null ? req.getMessage() : "仓储入库失败");
        }
        return result(plan, failed);
    }

    /** 入队或立即调度 AGV startTask，并持久化计划状态。 */
    private void sendAndEnqueue(AgvReturnPlanDO plan, String agvTaskId,
                                String operation, Map<String, Object> params) {
        plan.setCurrentAgvTaskId(agvTaskId);
        plan.setCurrentAgvOperation(operation);
        plan.setStatus(agvQueueService.enqueueOrDispatch(plan, agvTaskId, operation, params));
        planMapper.updateById(plan);
    }

    private Map<String, Object> result(AgvReturnPlanDO plan, AgvReturnItemDO item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", plan.getTaskId());
        result.put("slotId", item.getTransitSlotId());
        result.put("sequenceNo", item.getSequenceNo());
        result.put("planStatus", plan.getStatus());
        result.put("returnedCount", plan.getReturnedCount());
        result.put("expectedCount", plan.getExpectedCount());
        result.put("currentWave", plan.getCurrentWave());
        return result;
    }

    private int positiveInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        int parsed = Integer.parseInt(String.valueOf(value));
        if (parsed <= 0) throw exception(AGV_LOAD_PLAN_INVALID, "数值必须大于0: " + value);
        return parsed;
    }

    private String required(Map<String, Object> map, String key) {
        String value = string(map.get(key), null);
        if (value == null) throw exception(AGV_LOAD_PLAN_INVALID, key + "不能为空");
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requiredMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Map<?, ?>)) throw exception(AGV_LOAD_PLAN_INVALID, key + "不能为空");
        return (Map<String, Object>) value;
    }

    private String string(Object value, String defaultValue) {
        if (value == null || value.toString().isBlank()) return defaultValue;
        return value.toString();
    }
}
