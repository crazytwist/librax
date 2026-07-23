package com.librax.lab.module.resource.service.agvload;

import com.alibaba.fastjson.JSON;
import com.librax.lab.framework.common.exception.ServiceException;
import com.librax.lab.module.device.api.AgvTaskStateHandler;
import com.librax.lab.module.device.api.AgvWaitSignalHandler;
import com.librax.lab.module.device.api.AgvResumeHandler;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.resource.controller.app.slotinfo.vo.WarehouseCallbackReqVO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadItemDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadPlanDO;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadItemMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvTaskQueueMapper;
import com.librax.lab.module.resource.dal.mysql.slotinfo.SlotInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.*;

/**
 * 装载循环采用持久化屏障：AGV_WAITING + SOURCE_READY -> startAgain。
 *
 * <p>第一波由 startTask 执行；AGV 每次再次 waitSignal，代表上一波已经取料完成。
 * 中间波次由本服务直接 startAgain，最后一波完成后才唤醒流程的 WAIT 节点，
 * 由原 DAG 下发最后一次 startAgain 进入移动/卸载阶段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgvLoadPlanServiceImpl implements AgvLoadPlanService, AgvWaitSignalHandler, AgvTaskStateHandler, AgvResumeHandler {

    /** 自注入，用于在事务提交后通过 Spring 代理调用 @Async 方法 */
    @Lazy
    @Autowired
    private AgvLoadPlanService self;

    private static final String FLOW_WAIT_NODE = "s_agv_wait_signal";

    private final AgvLoadPlanMapper planMapper;
    private final AgvLoadItemMapper itemMapper;
    private final AgvTaskQueueMapper queueMapper;
    private final SlotInfoMapper slotMapper;
    private final DeviceCommandSpi deviceCommandSpi;
    private final StepCallbackSpi stepCallbackSpi;
    private final AgvQueueService agvQueueService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWarehouseOrchestration(String taskId,
                                            String nodeId,
                                            List<Map<String, Object>> transferItems,
                                            Map<String, Object> inputParams) {
        if (planMapper.selectByTaskId(taskId) != null) return;
        if (transferItems == null || transferItems.isEmpty()) {
            throw exception(AGV_LOAD_PLAN_INVALID, "transferItems不能为空");
        }
        int waveSize = positiveInt(inputParams.get("sourceWindowCapacity"), 2);
        int agvCapacity = positiveInt(inputParams.get("agvCapacity"), 8);
        if (transferItems.size() > agvCapacity) {
            throw exception(AGV_LOAD_PLAN_INVALID,
                    "单车物料数" + transferItems.size() + "超过AGV容量" + agvCapacity);
        }

        List<String> reserved = new ArrayList<>();
        boolean targetPreReserved = bool(inputParams.get("targetPreReserved"));
        for (Map<String, Object> item : transferItems) {
            String agvSlot = required(item, "agvSlotId");
            String target = required(item, "targetSlotId");
            if (slotMapper.reserveIfEmpty(agvSlot) == 0) {
                throw exception(AGV_LOAD_PLAN_INVALID, "AGV位置不可用: " + agvSlot);
            }
            reserved.add(agvSlot);
            if (targetPreReserved) {
                SlotInfoDO targetSlot = slotMapper.selectBySlotId(target);
                if (targetSlot == null || !"RESERVED".equals(targetSlot.getStatus())) {
                    throw exception(AGV_LOAD_PLAN_INVALID, "目标位置未按补料单预留: " + target);
                }
            } else if (slotMapper.reserveIfEmpty(target) == 0) {
                throw exception(AGV_LOAD_PLAN_INVALID, "目标位置不可用: " + target);
            }
            reserved.add(target);
        }

        AgvLoadPlanDO plan = AgvLoadPlanDO.builder()
                .taskId(taskId)
                .deviceId(string(inputParams.get("agvDeviceId"), "AGV-01"))
                .loadStation(string(inputParams.get("loadStation"), null))
                .expectedCount(transferItems.size())
                .loadedCount(0)
                .waveSize(waveSize)
                .currentWave(1)
                .allowPartialLoad(false)
                .agvWaiting(false)
                .orchestrationNodeId(nodeId)
                .taskType(string(inputParams.get("taskType"), "1"))
                .plateType(string(inputParams.get("plateType"), "Plate_5"))
                .warehouseCallbackUrl(string(inputParams.get("warehouseCallbackUrl"),
                        "http://localhost:48080/app-api/resource/slot/ready"))
                .status("WAIT_WAREHOUSE")
                .build();
        planMapper.insert(plan);

        for (int i = 0; i < transferItems.size(); i++) {
            Map<String, Object> item = transferItems.get(i);
            itemMapper.insert(AgvLoadItemDO.builder()
                    .taskId(taskId)
                    .sequenceNo(i + 1)
                    .waveNo(i / waveSize + 1)
                    .sourceSlotId(required(item, "sourceSlotId"))
                    .warehouseSourceLocation(required(item, "warehouseSourceLocation"))
                    .agvSlotId(required(item, "agvSlotId"))
                    .targetSlotId(required(item, "targetSlotId"))
                    .instanceId(required(item, "instanceId"))
                    .containerType(string(item.get("containerType"), null))
                    .step1Json(JSON.toJSONString(requiredMap(item, "step1")))
                    .step2Json(item.get("step2") instanceof Map<?, ?> ? JSON.toJSONString(item.get("step2")) : null)
                    .step3Json(item.get("step3") instanceof Map<?, ?> ? JSON.toJSONString(item.get("step3")) : null)
                    .status("WAIT_SOURCE")
                    .build());
        }
        requestNextWarehouseItem(plan);
        log.info("[AgvWarehouse] 启动仓储驱动搬运 taskId={} count={} waveSize={}",
                taskId, transferItems.size(), waveSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerIfEnabled(String taskId,
                                  List<Map<String, Object>> transferItems,
                                  Map<String, Object> inputParams) {
        if (!bool(inputParams.get("multiLoadEnabled"))) return;
        if (planMapper.selectByTaskId(taskId) != null) return;
        if (!bool(inputParams.get("waitSignalEnabled"))) {
            throw exception(AGV_LOAD_PLAN_INVALID,
                    "multiLoadEnabled=true时waitSignalEnabled必须为true");
        }
        if (transferItems == null || transferItems.isEmpty()) {
            throw exception(AGV_LOAD_PLAN_INVALID, "transferItems不能为空");
        }

        int waveSize = positiveInt(inputParams.get("sourceWindowCapacity"), 2);
        int agvCapacity = positiveInt(inputParams.get("agvCapacity"), 8);
        if (transferItems.size() > agvCapacity) {
            throw exception(AGV_LOAD_PLAN_INVALID,
                    "单车物料数" + transferItems.size() + "超过AGV容量" + agvCapacity);
        }
        int firstWaveCount = Math.min(waveSize, transferItems.size());
        Set<String> firstWaveSources = new HashSet<>();

        AgvLoadPlanDO plan = AgvLoadPlanDO.builder()
                .taskId(taskId)
                .deviceId(string(inputParams.get("agvDeviceId"), "AGV-01"))
                .loadStation(string(inputParams.get("loadStation"), null))
                .expectedCount(transferItems.size())
                .loadedCount(0)
                .waveSize(waveSize)
                .currentWave(1)
                .allowPartialLoad(bool(inputParams.get("allowPartialLoad")))
                .agvWaiting(false)
                .status("LOADING")
                .build();
        planMapper.insert(plan);

        for (int i = 0; i < transferItems.size(); i++) {
            Map<String, Object> item = transferItems.get(i);
            String sourceSlotId = required(item, "sourceSlotId");
            String instanceId = string(item.get("instanceId"), null);
            String status = "WAIT_SOURCE";
            LocalDateTime dispatchedAt = null;
            if (i < firstWaveCount) {
                if (!firstWaveSources.add(sourceSlotId)) {
                    throw exception(AGV_LOAD_PLAN_INVALID, "首波源库位重复: " + sourceSlotId);
                }
                SlotInfoDO source = slotMapper.selectBySlotId(sourceSlotId);
                if (source == null || !"OCCUPIED".equals(source.getStatus())) {
                    throw exception(AGV_LOAD_PLAN_INVALID, "首波源库位未就绪: " + sourceSlotId);
                }
                if (source.getOccupiedBy() != null && !source.getOccupiedBy().isBlank()) {
                    instanceId = source.getOccupiedBy();
                }
                status = "DISPATCHED"; // startTask 负责执行第一波
                dispatchedAt = LocalDateTime.now();
            }
            itemMapper.insert(AgvLoadItemDO.builder()
                    .taskId(taskId)
                    .sequenceNo(i + 1)
                    .sourceSlotId(sourceSlotId)
                    .agvSlotId(required(item, "agvSlotId"))
                    .targetSlotId(required(item, "targetSlotId"))
                    .instanceId(instanceId)
                    .status(status)
                    .dispatchedAt(dispatchedAt)
                    .build());
        }
        log.info("[AgvLoadPlan] 创建多轮装载计划 taskId={} count={} waveSize={}",
                taskId, transferItems.size(), waveSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> markSlotReady(WarehouseCallbackReqVO req) {
        // 纯货架独立出货：无 AGV 任务上下文，只更新库位状态
        if (!org.springframework.util.StringUtils.hasText(req.getTransferTaskId())) {
            updateSlotStatus(req);
            log.info("[SlotReady] 独立出货 slotId={} instanceId={} status={}",
                    req.getToLocation(), req.getMaterialId(), req.getStatus());
            return Map.of("standalone", true, "slotId", req.getToLocation(), "status", req.getStatus());
        }

        AgvLoadItemDO duplicated = itemMapper.selectByReadyRequestId(req.getRequestId());
        if (duplicated != null) {
            if (!Objects.equals(duplicated.getTaskId(), req.getTransferTaskId())
                    || !Objects.equals(duplicated.getSourceSlotId(), req.getToLocation())) {
                throw exception(SLOT_READY_CONFLICT, "requestId已被其它任务或库位使用");
            }
            AgvLoadPlanDO plan = planMapper.selectByTaskId(req.getTransferTaskId());
            if (plan == null) throw exception(AGV_LOAD_PLAN_NOT_EXISTS, req.getTransferTaskId());
            return result(plan, duplicated, false, true);
        }

        AgvLoadPlanDO plan = requireActivePlanForUpdate(req.getTransferTaskId());
        if (!req.isSuccess()) {
            AgvLoadItemDO failed = itemMapper.selectByTaskIdAndSourceSlot(req.getTransferTaskId(), req.getToLocation());
            if (failed == null) {
                throw exception(SLOT_READY_CONFLICT, "找不到对应的仓储备料请求");
            }
            failed.setStatus("FAILED");
            failed.setReadyRequestId(req.getRequestId());
            itemMapper.updateById(failed);
            slotMapper.clearOccupancy(failed.getSourceSlotId());
            plan.setStatus("FAILED");
            planMapper.updateById(plan);
            if (plan.getOrchestrationNodeId() != null) {
                stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null,
                        false, Map.of("requestId", req.getRequestId()),
                        "WAREHOUSE_" + (req.getCode() != null ? req.getCode() : 1099),
                        req.getMessage() != null ? req.getMessage() : "仓储备料失败");
            }
            return result(plan, failed, false, false);
        }

        AgvLoadItemDO item = itemMapper.selectNextWaiting(req.getTransferTaskId(), req.getToLocation());
        if (item == null) {
            throw exception(SLOT_READY_CONFLICT,
                    "任务" + req.getTransferTaskId() + "没有等待" + req.getToLocation() + "的装载项");
        }

        SlotInfoDO slot = slotMapper.selectBySlotId(req.getToLocation());
        if (slot == null) throw exception(SLOT_INFO_NOT_EXISTS);
        if (!Boolean.TRUE.equals(slot.getEnabled()) || "DISABLED".equals(slot.getStatus())) {
            throw exception(SLOT_DISABLED);
        }
        if ("RESERVED".equals(slot.getStatus())) {
            if (slotMapper.markReadyIfReserved(req.getToLocation(), req.getMaterialId()) == 0) {
                throw exception(SLOT_READY_CONFLICT, "库位预留状态发生并发变化: " + req.getToLocation());
            }
        } else if ("EMPTY".equals(slot.getStatus())) {
            if (slotMapper.markReadyIfEmpty(req.getToLocation(), req.getMaterialId()) == 0) {
                throw exception(SLOT_READY_CONFLICT, "库位状态发生并发变化: " + req.getToLocation());
            }
        } else if (!"OCCUPIED".equals(slot.getStatus())
                || !Objects.equals(slot.getOccupiedBy(), req.getMaterialId())) {
            throw exception(SLOT_READY_CONFLICT,
                    req.getToLocation() + "当前由" + slot.getOccupiedBy() + "占用");
        }

        item.setInstanceId(req.getMaterialId());
        item.setReadyRequestId(req.getRequestId());
        item.setReadyAt(req.getEventTime() != null ? req.getEventTime() : LocalDateTime.now());
        item.setStatus("READY");
        itemMapper.updateById(item);

        // 事务提交后再异步触发下一步，确保先返回200给仓储，再发下一条 prepareMaterials
        String planTaskId = plan.getTaskId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.triggerNextStepAsync(planTaskId);
            }
        });
        log.info("[AgvLoadPlan] 源库位就绪 taskId={} slotId={} instanceId={} 异步触发下一步",
                req.getTransferTaskId(), req.getToLocation(), req.getMaterialId());
        return result(plan, item, false, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String taskId, String agvStation) {
        AgvLoadPlanDO subtaskPlan = planMapper.selectByCurrentAgvTaskId(taskId);
        if (subtaskPlan != null && subtaskPlan.getOrchestrationNodeId() != null
                && subtaskPlan.getStatus().startsWith("WAIT_AGV_")) {
            subtaskPlan.setStatus("WAIT_SITE_ACTION");
            subtaskPlan.setLastAgvStation(agvStation);
            planMapper.updateById(subtaskPlan);
            log.info("[AgvWarehouse] AGV进入现场动作等待 taskId={} operation={} station={}",
                    taskId, subtaskPlan.getCurrentAgvOperation(), agvStation);
            return true;
        }
        AgvLoadPlanDO existing = planMapper.selectByTaskId(taskId);
        if (existing == null || !"LOADING".equals(existing.getStatus())) return false;
        if (existing.getLoadStation() != null
                && !existing.getLoadStation().equals(agvStation)) return false;

        AgvLoadPlanDO plan = requireActivePlanForUpdate(taskId);
        completeDispatchedWave(plan);
        plan.setLastAgvStation(agvStation);

        if (plan.getLoadedCount() >= plan.getExpectedCount()) {
            plan.setStatus("LOAD_COMPLETE");
            plan.setAgvWaiting(false);
            planMapper.updateById(plan);
            CallbackResult callback = stepCallbackSpi.callback(
                    taskId, FLOW_WAIT_NODE, null, true,
                    Map.of("signal", "LOAD_COMPLETE",
                            "loadedCount", plan.getLoadedCount(),
                            "agvStation", agvStation),
                    null, null);
            if (!callback.isSuccess()) {
                throw new ServiceException(AGV_LOAD_PLAN_STATE_INVALID)
                        .setMessage("装载完成但流程WAIT节点推进失败: " + callback.getErrorMsg());
            }
            log.info("[AgvLoadPlan] 当前车次装载完成 taskId={} count={}",
                    taskId, plan.getLoadedCount());
            return true;
        }

        plan.setAgvWaiting(true);
        planMapper.updateById(plan);
        dispatchNextWaveIfReady(plan);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(String agvTaskId) {
        AgvLoadPlanDO existing = planMapper.selectByCurrentAgvTaskId(agvTaskId);
        if (existing == null || !"WAIT_SITE_ACTION".equals(existing.getStatus())) return false;
        AgvLoadPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(agvTaskId);
        deviceCommandSpi.send("AGV", "startAgain",
                Map.of("taskId", agvTaskId, "disableSysFieldInjection", true),
                plan.getTaskId(), "resume_" + plan.getCurrentAgvOperation(), null);
        plan.setStatus("WAIT_AGV_" + plan.getCurrentAgvOperation());
        planMapper.updateById(plan);
        log.info("[AgvWarehouse] 现场动作完成，通知AGV继续 taskId={} operation={}",
                agvTaskId, plan.getCurrentAgvOperation());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String agvTaskId, String runState, String agvId) {
        AgvLoadPlanDO existing = planMapper.selectByCurrentAgvTaskId(agvTaskId);
        if (existing == null || existing.getOrchestrationNodeId() == null) return false;
        if ("1".equals(runState)) return true;

        AgvLoadPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(agvTaskId);
        if (!"2".equals(runState)) {
            plan.setStatus("FAILED");
            planMapper.updateById(plan);
            queueMapper.cancelByAgvTaskId(agvTaskId);
            stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null,
                    false, Map.of("agvTaskId", agvTaskId, "runState", runState),
                    "AGV_TASK_ERROR", "AGV子任务失败 runState=" + runState);
            agvQueueService.dispatchNext(plan.getDeviceId());
            return true;
        }

        queueMapper.markDoneByAgvTaskId(agvTaskId); // 先标记完成，让 AGV "释放"

        switch (plan.getCurrentAgvOperation()) {
            case "LOAD" -> completeLoadAndContinue(plan);   // 内部可能调 sendMoveTask，走队列
            case "MOVE" -> sendUnloadTask(plan);             // 走队列
            case "UNLOAD" -> {
                completeOrchestration(plan);
                agvQueueService.dispatchNext(plan.getDeviceId());
            }
            default -> throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                    "未知AGV子任务类型: " + plan.getCurrentAgvOperation());
        }
        return true;
    }

    @Override
    public boolean hasPlan(String taskId) {
        return planMapper.selectByTaskId(taskId) != null;
    }

    @Override
    public Map<String, Object> getProgress(String taskId) {
        AgvLoadPlanDO plan = planMapper.selectByTaskId(taskId);
        if (plan == null) throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
        List<Map<String, Object>> items = itemMapper.selectByTaskId(taskId).stream().map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("sequenceNo", item.getSequenceNo());
            value.put("sourceSlotId", item.getSourceSlotId());
            value.put("agvSlotId", item.getAgvSlotId());
            value.put("targetSlotId", item.getTargetSlotId());
            value.put("instanceId", item.getInstanceId());
            value.put("status", item.getStatus());
            return value;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", plan.getStatus());
        result.put("loadedCount", plan.getLoadedCount());
        result.put("expectedCount", plan.getExpectedCount());
        result.put("currentWave", plan.getCurrentWave());
        result.put("agvWaiting", plan.getAgvWaiting());
        result.put("lastAgvStation", plan.getLastAgvStation());
        result.put("items", items);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void commitTransfer(String taskId) {
        AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null) throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
        if (!Set.of("LOAD_COMPLETE", "WAIT_FLOW_COMMIT").contains(plan.getStatus())) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, plan.getStatus());
        }
        List<AgvLoadItemDO> items = itemMapper.selectByTaskId(taskId);
        for (AgvLoadItemDO item : items) {
            if (!"LOADED".equals(item.getStatus())) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                        "物料" + item.getSequenceNo() + "状态=" + item.getStatus());
            }
            slotMapper.clearOccupancy(item.getAgvSlotId());
            if (slotMapper.occupyReserved(item.getTargetSlotId(), item.getInstanceId()) == 0) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                        "目标库位预留已丢失: " + item.getTargetSlotId());
            }
            item.setStatus("COMPLETED");
            itemMapper.updateById(item);
        }
        plan.setStatus("COMPLETED");
        planMapper.updateById(plan);
    }

    private void requestNextWarehouseItem(AgvLoadPlanDO plan) {
        AgvLoadItemDO next = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), plan.getCurrentWave()))
                .filter(item -> "WAIT_SOURCE".equals(item.getStatus()))
                .filter(item -> item.getWarehouseRequestId() == null)
                .findFirst().orElse(null);
        if (next == null) return;
        if (slotMapper.reserveIfEmpty(next.getSourceSlotId()) == 0) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                    "仓储中转位不可用: " + next.getSourceSlotId());
        }
        String subRequestId = plan.getTaskId() + "-W" + next.getWaveNo() + "-I" + next.getSequenceNo();
        next.setWarehouseRequestId(subRequestId);
        itemMapper.updateById(next);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("requestId", subRequestId);                        // 幂等键，仓储回调时原样带回
        params.put("transferTaskId", plan.getTaskId());               // 流程ID，回调时作为 taskId 返回
        params.put("materialId", next.getInstanceId());               // 物料实例ID，仓储据此取货
        params.put("containerType", next.getContainerType());         // 容器类型
        params.put("fromLocation", next.getWarehouseSourceLocation()); // 货架源位置
        params.put("toLocation", next.getSourceSlotId());             // 中转位（AGV来此取料）
        params.put("callbackUrl", plan.getWarehouseCallbackUrl());
        params.put("disableSysFieldInjection", true);
        int maxRetries = 3;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                long delayMs = 1000L * (1L << (attempt - 1)); // 1s, 2s, 4s
                log.warn("[AgvWarehouse] 备料重试 taskId={} attempt={}/{} waitMs={}",
                        plan.getTaskId(), attempt, maxRetries, delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw exception(AGV_LOAD_PLAN_STATE_INVALID, "备料重试被中断");
                }
            }
            DeviceCommandSpi.Result response = deviceCommandSpi.send("WAREHOUSE", "prepareMaterials", params,
                    plan.getTaskId(), "warehouse_prepare_" + next.getSequenceNo(), null);
            if (response.getResponseBody() == null || response.getResponseBody().isBlank()) {
                break; // 无响应体，视为接受
            }
            Integer code = JSON.parseObject(response.getResponseBody()).getInteger("code");
            if (code == null || code == 0 || code == 1005) {
                break; // 成功
            }
            String message = JSON.parseObject(response.getResponseBody()).getString("message");
            if (message == null) message = JSON.parseObject(response.getResponseBody()).getString("msg");
            log.warn("[AgvWarehouse] 仓储拒绝备料 taskId={} attempt={} code={} message={}",
                    plan.getTaskId(), attempt, code, message);
            if (attempt == maxRetries) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                        "仓储拒绝备料(已重试" + maxRetries + "次) code=" + code + ", message=" + message);
            }
        }
        log.info("[AgvWarehouse] 请求仓储备料成功 taskId={} subRequestId={} from={} to={}",
                plan.getTaskId(), subRequestId, next.getWarehouseSourceLocation(), next.getSourceSlotId());
    }

    private boolean afterWarehouseReady(AgvLoadPlanDO plan) {
        List<AgvLoadItemDO> waveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), plan.getCurrentWave()))
                .toList();
        long ready = waveItems.stream().filter(item -> "READY".equals(item.getStatus())).count();
        if (ready < waveItems.size()) {
            requestNextWarehouseItem(plan); // 单机械臂：收到一个回调后才请求下一个
            return false;
        }
        sendLoadTask(plan, waveItems);
        return true;
    }

    @SuppressWarnings("unchecked")
    private void sendLoadTask(AgvLoadPlanDO plan, List<AgvLoadItemDO> waveItems) {
        String agvTaskId = plan.getTaskId() + "-LOAD-" + plan.getCurrentWave();
        List<Map<String, Object>> commands = new ArrayList<>();
        for (AgvLoadItemDO item : waveItems) {
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("taskType", plan.getTaskType());
            command.put("plateType", plan.getPlateType());
            command.put("step1", JSON.parseObject(item.getStep1Json(), Map.class));
            commands.add(command);
            item.setStatus("DISPATCHED");
            item.setDispatchedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        sendAndEnqueue(plan, agvTaskId, "LOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", commands, "disableSysFieldInjection", true));
    }

    private void completeLoadAndContinue(AgvLoadPlanDO plan) {
        List<AgvLoadItemDO> waveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), plan.getCurrentWave()))
                .filter(item -> "DISPATCHED".equals(item.getStatus()))
                .toList();
        for (AgvLoadItemDO item : waveItems) {
            slotMapper.clearOccupancy(item.getSourceSlotId());
            if (slotMapper.occupyReserved(item.getAgvSlotId(), item.getInstanceId()) == 0) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                        "AGV库位预留已丢失: " + item.getAgvSlotId());
            }
            item.setStatus("LOADED");
            item.setLoadedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        plan.setLoadedCount(plan.getLoadedCount() + waveItems.size());
        if (plan.getLoadedCount() < plan.getExpectedCount()) {
            plan.setCurrentWave(plan.getCurrentWave() + 1);
            plan.setCurrentAgvTaskId(null);
            plan.setCurrentAgvOperation(null);
            plan.setStatus("WAIT_WAREHOUSE");
            planMapper.updateById(plan);
            requestNextWarehouseItem(plan);
        } else {
            sendMoveTask(plan);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendMoveTask(AgvLoadPlanDO plan) {
        AgvLoadItemDO source = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> item.getStep2Json() != null).findFirst()
                .orElseThrow(() -> exception(AGV_LOAD_PLAN_INVALID, "step2不能为空"));
        String agvTaskId = plan.getTaskId() + "-MOVE";
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("taskType", plan.getTaskType());
        command.put("plateType", plan.getPlateType());
        command.put("step2", JSON.parseObject(source.getStep2Json(), Map.class));
        sendAndEnqueue(plan, agvTaskId, "MOVE",
                Map.of("taskId", agvTaskId, "agvCmdList", List.of(command), "disableSysFieldInjection", true));
    }

    @SuppressWarnings("unchecked")
    private void sendUnloadTask(AgvLoadPlanDO plan) {
        String agvTaskId = plan.getTaskId() + "-UNLOAD";
        List<Map<String, Object>> commands = new ArrayList<>();
        for (AgvLoadItemDO item : itemMapper.selectByTaskId(plan.getTaskId())) {
            if (item.getStep3Json() == null) continue;
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("taskType", plan.getTaskType());
            command.put("plateType", plan.getPlateType());
            command.put("step3", JSON.parseObject(item.getStep3Json(), Map.class));
            commands.add(command);
        }
        if (commands.isEmpty()) throw exception(AGV_LOAD_PLAN_INVALID, "step3不能为空");
        sendAndEnqueue(plan, agvTaskId, "UNLOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", commands, "disableSysFieldInjection", true));
    }

    private void completeOrchestration(AgvLoadPlanDO plan) {
        plan.setStatus("WAIT_FLOW_COMMIT");
        planMapper.updateById(plan);
        CallbackResult callback = stepCallbackSpi.callback(
                plan.getTaskId(), plan.getOrchestrationNodeId(), null, true,
                Map.of("loadedCount", plan.getLoadedCount(), "warehouseOrchestration", true),
                null, null);
        if (!callback.isSuccess()) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                    "流程编排节点推进失败: " + callback.getErrorMsg());
        }
    }

    private void completeDispatchedWave(AgvLoadPlanDO plan) {
        List<AgvLoadItemDO> dispatched = itemMapper
                .selectByTaskIdAndStatus(plan.getTaskId(), "DISPATCHED");
        if (dispatched.isEmpty()) return;
        for (AgvLoadItemDO item : dispatched) {
            slotMapper.clearOccupancy(item.getSourceSlotId());
            if (slotMapper.occupyReserved(item.getAgvSlotId(), item.getInstanceId()) == 0) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                        "AGV库位预留已丢失: " + item.getAgvSlotId());
            }
            item.setStatus("LOADED");
            item.setLoadedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        plan.setLoadedCount(plan.getLoadedCount() + dispatched.size());
        planMapper.updateById(plan);
    }

    private boolean dispatchNextWaveIfReady(AgvLoadPlanDO plan) {
        if (!Boolean.TRUE.equals(plan.getAgvWaiting())) return false;
        int remaining = plan.getExpectedCount() - plan.getLoadedCount();
        int expected = Math.min(plan.getWaveSize(), remaining);
        List<AgvLoadItemDO> ready = itemMapper.selectByTaskIdAndStatus(plan.getTaskId(), "READY");
        if (ready.isEmpty()) return false;
        if (!Boolean.TRUE.equals(plan.getAllowPartialLoad()) && ready.size() < expected) return false;

        int dispatchCount = Boolean.TRUE.equals(plan.getAllowPartialLoad())
                ? Math.min(expected, ready.size()) : expected;
        List<AgvLoadItemDO> wave = ready.subList(0, dispatchCount);
        int nextWave = plan.getCurrentWave() + 1;
        LocalDateTime now = LocalDateTime.now();
        for (AgvLoadItemDO item : wave) {
            item.setStatus("DISPATCHED");
            item.setDispatchedAt(now);
            itemMapper.updateById(item);
        }
        plan.setCurrentWave(nextWave);
        plan.setAgvWaiting(false);
        planMapper.updateById(plan);

        deviceCommandSpi.send("AGV", "startAgain",
                Map.of("taskId", plan.getTaskId(), "disableSysFieldInjection", true),
                plan.getTaskId(), "agv_load_wave_" + nextWave, null);
        log.info("[AgvLoadPlan] 第{}波就绪，通知AGV继续 taskId={} count={}",
                nextWave, plan.getTaskId(), dispatchCount);
        return true;
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void triggerNextStepAsync(String taskId) {
        try {
            // 等待 200ms，给仓储设备完成内部状态切换的时间
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            AgvLoadPlanDO plan = planMapper.selectByTaskId(taskId);
            if (plan == null) {
                log.warn("[AgvLoadPlan] 异步触发下一步：计划不存在 taskId={}", taskId);
                return;
            }
            boolean triggered = plan.getOrchestrationNodeId() != null
                    ? afterWarehouseReady(plan) : dispatchNextWaveIfReady(plan);
            log.info("[AgvLoadPlan] 异步触发下一步完成 taskId={} triggered={}", taskId, triggered);
        } catch (Exception e) {
            log.error("[AgvLoadPlan] 异步触发下一步失败 taskId={}", taskId, e);
        }
    }

    private AgvLoadPlanDO requireActivePlanForUpdate(String taskId) {
        AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null) throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
        if (!Set.of("LOADING", "WAIT_WAREHOUSE").contains(plan.getStatus())) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, plan.getStatus());
        }
        return plan;
    }

    private Map<String, Object> result(AgvLoadPlanDO plan, AgvLoadItemDO item,
                                       boolean triggered, boolean duplicated) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", plan.getTaskId());
        result.put("slotId", item.getSourceSlotId());
        result.put("sequenceNo", item.getSequenceNo());
        result.put("planStatus", plan.getStatus());
        result.put("loadedCount", plan.getLoadedCount());
        result.put("expectedCount", plan.getExpectedCount());
        result.put("agvWaiting", plan.getAgvWaiting());
        result.put("startAgainTriggered", triggered);
        result.put("duplicated", duplicated);
        return result;
    }

    private int positiveInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        int parsed = Integer.parseInt(String.valueOf(value));
        if (parsed <= 0) throw exception(AGV_LOAD_PLAN_INVALID, "数值必须大于0: " + value);
        return parsed;
    }

    private boolean bool(Object value) {
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value));
    }

    private String required(Map<String, Object> map, String key) {
        String value = string(map.get(key), null);
        if (value == null) throw exception(AGV_LOAD_PLAN_INVALID, key + "不能为空");
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requiredMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Map<?, ?>)) {
            throw exception(AGV_LOAD_PLAN_INVALID, key + "不能为空");
        }
        return (Map<String, Object>) value;
    }

    private String string(Object value, String defaultValue) {
        if (value == null || value.toString().isBlank()) return defaultValue;
        return value.toString();
    }

    /** 入队或立即调度 AGV startTask，并持久化计划状态。 */
    private void sendAndEnqueue(AgvLoadPlanDO plan, String agvTaskId,
                                String operation, Map<String, Object> params) {
        plan.setCurrentAgvTaskId(agvTaskId);
        plan.setCurrentAgvOperation(operation);
        plan.setStatus(agvQueueService.enqueueOrDispatch(plan, agvTaskId, operation, params));
        planMapper.updateById(plan);
    }

    /** 纯货架独立出货时只更新库位占用状态，不涉及 AGV 计划。 */
    private void updateSlotStatus(WarehouseCallbackReqVO req) {
        SlotInfoDO slot = slotMapper.selectBySlotId(req.getToLocation());
        if (slot == null) throw exception(SLOT_INFO_NOT_EXISTS);
        if (!Boolean.TRUE.equals(slot.getEnabled()) || "DISABLED".equals(slot.getStatus())) {
            throw exception(SLOT_DISABLED);
        }
        if (!req.isSuccess()) return; // FAILED 时不更新库位
        if ("RESERVED".equals(slot.getStatus())) {
            slotMapper.markReadyIfReserved(req.getToLocation(), req.getMaterialId());
        } else if ("EMPTY".equals(slot.getStatus())) {
            slotMapper.markReadyIfEmpty(req.getToLocation(), req.getMaterialId());
        }
    }
}
