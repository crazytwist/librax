package com.librax.lab.module.resource.service.agvload;

import com.alibaba.fastjson.JSON;
import com.librax.lab.framework.common.exception.ServiceException;
import com.librax.lab.module.device.api.AgvDoorPermissionHandler;
import com.librax.lab.module.device.api.AgvTaskStateHandler;
import com.librax.lab.module.device.api.AgvWaitSignalHandler;
import com.librax.lab.module.device.api.AgvResumeHandler;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.flow.api.material.MaterialLocationSpi;
import com.librax.lab.module.flow.api.pipeline.PipelineExecutionSpi;
import com.librax.lab.module.resource.controller.app.slotinfo.vo.WarehouseCallbackReqVO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadItemDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadPlanDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvOrchestrateQueueDO;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadItemMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvOrchestrateQueueMapper;
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
import java.util.stream.Collectors;

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
public class AgvLoadPlanServiceImpl
        implements AgvLoadPlanService, AgvWaitSignalHandler, AgvTaskStateHandler, AgvResumeHandler,
        AgvDoorPermissionHandler {

    /**
     * 自注入，用于在事务提交后通过 Spring 代理调用 @Async 方法
     */
    @Lazy
    @Autowired
    private AgvLoadPlanService self;

    private static final String FLOW_WAIT_NODE = "s_agv_wait_signal";

    private final AgvLoadPlanMapper planMapper;
    private final AgvLoadItemMapper itemMapper;
    private final AgvTaskQueueMapper queueMapper;
    private final AgvOrchestrateQueueMapper orchestrateQueueMapper;
    private final SlotInfoMapper slotMapper;
    private final DeviceCommandSpi deviceCommandSpi;
    private final StepCallbackSpi stepCallbackSpi;
    private final MaterialLocationSpi materialLocationSpi;
    private final AgvQueueService agvQueueService;
    private final PipelineExecutionSpi executionSpi;

    @Override
    public void startWarehouseOrchestration(String taskId, String nodeId, List<Map<String, Object>> transferItems,
            Map<String, Object> inputParams) {
        if (planMapper.selectByTaskId(taskId) != null)
            return;
        if (orchestrateQueueMapper.selectByExecutionId(taskId) != null) {
            log.info("[AgvWarehouse] taskId={} 已在编排排队队列中，忽略重复请求", taskId);
            return;
        }
        try {
            self.attemptStartWarehouseOrchestration(taskId, nodeId, transferItems, inputParams);
        } catch (ServiceException e) {
            if (!AGV_SLOT_UNAVAILABLE.getCode().equals(e.getCode())) {
                throw e; // 非槽位冲突（参数错误、容量超限等）维持原有硬失败行为，不排队
            }
            enqueueOrchestrateRequest("LOAD", string(inputParams.get("agvDeviceId"), "AGV-01"), taskId, nodeId,
                    transferItems, inputParams, e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void attemptStartWarehouseOrchestration(String taskId, String nodeId,
            List<Map<String, Object>> transferItems, Map<String, Object> inputParams) {
        if (planMapper.selectByTaskId(taskId) != null)
            return;
        if (transferItems == null || transferItems.isEmpty()) {
            throw exception(AGV_LOAD_PLAN_INVALID, "transferItems不能为空");
        }
        // sourceWindowCapacity 语义：WAREHOUSE模式下是"仓储备料并发窗口"（同时最多备几件，
        // 跟AGV一次抓几个无关，AGV永远逐件抓取，见 afterWarehouseReady/tryDispatchReadyItem/
        // refillWarehouseWindow）；DIRECT模式下沿用老语义，就是"整波打包给AGV的物料数"。
        // 调用方不传时默认1（保守：没有并发备料能力时不做预取）。
        int waveSize = positiveInt(inputParams.get("sourceWindowCapacity"), 1);
        int agvCapacity = positiveInt(inputParams.get("agvCapacity"), 8);
        if (transferItems.size() > agvCapacity) {
            throw exception(AGV_LOAD_PLAN_INVALID, "单车物料数" + transferItems.size() + "超过AGV容量" + agvCapacity);
        }
        String sourceReadyMode = string(inputParams.get("sourceReadyMode"), "WAREHOUSE").toUpperCase();

        List<String> reserved = new ArrayList<>();
        boolean targetPreReserved = bool(inputParams.get("targetPreReserved"));
        for (Map<String, Object> item : transferItems) {
            String agvSlot = required(item, "agvSlotId");
            String target = required(item, "targetSlotId");
            if (slotMapper.reserveIfEmpty(agvSlot) == 0) {
                throw exception(AGV_SLOT_UNAVAILABLE, "AGV位置不可用: " + agvSlot);
            }
            reserved.add(agvSlot);
            if (targetPreReserved) {
                SlotInfoDO targetSlot = slotMapper.selectBySlotId(target);
                if (targetSlot == null || !"RESERVED".equals(targetSlot.getStatus())) {
                    throw exception(AGV_LOAD_PLAN_INVALID, "目标位置未按补料单预留: " + target);
                }
            } else if (slotMapper.reserveIfEmpty(target) == 0) {
                throw exception(AGV_SLOT_UNAVAILABLE, "目标位置不可用: " + target);
            }
            reserved.add(target);
        }

        AgvLoadPlanDO plan = AgvLoadPlanDO.builder().taskId(taskId)
                .deviceId(string(inputParams.get("agvDeviceId"), "AGV-01")).loadStation(
                        string(inputParams.get("sourceAgvStationName"),
                                string(inputParams.get("loadStation"), "WAREHOUSE")))
                .expectedCount(transferItems.size()).loadedCount(0).waveSize(waveSize).currentWave(1)
                .allowPartialLoad(false).agvWaiting(false).orchestrationNodeId(nodeId)
                .taskType(string(inputParams.get("taskType"), "1"))
                .plateType(string(inputParams.get("plateType"), "Plate_5")).warehouseCallbackUrl(
                        string(inputParams.get("warehouseCallbackUrl"),
                                "http://localhost:48080/app-api/resource/slot/warehouse-callback"))
                .sourceReadyMode(sourceReadyMode).doorRequired(bool(inputParams.get("doorRequired")))
                .doorDeviceId(string(inputParams.get("doorDeviceId"), null))
                .doorCommandCode(string(inputParams.get("doorCommandCode"), null))
                .doorPermissionGateRequired(bool(inputParams.get("doorPermissionGateRequired")))
                .doorPermissionReceived(false).doorOpened(false).status("WAIT_WAREHOUSE").build();
        planMapper.insert(plan);

        for (int i = 0; i < transferItems.size(); i++) {
            Map<String, Object> item = transferItems.get(i);
            // DIRECT 模式下物料已在源库位上，warehouseSourceLocation 允许直接等于 sourceSlotId
            String sourceSlotId = required(item, "sourceSlotId");
            String warehouseSourceLocation = "DIRECT".equals(sourceReadyMode) ?
                    string(item.get("warehouseSourceLocation"), sourceSlotId) :
                    required(item, "warehouseSourceLocation");
            itemMapper.insert(AgvLoadItemDO.builder().taskId(taskId).sequenceNo(i + 1).waveNo(i / waveSize + 1)
                    .sourceSlotId(sourceSlotId).warehouseSourceLocation(warehouseSourceLocation)
                    .agvSlotId(required(item, "agvSlotId")).targetSlotId(required(item, "targetSlotId"))
                    .instanceId(required(item, "instanceId")).containerType(string(item.get("containerType"), null))
                    .step1Json(JSON.toJSONString(requiredMap(item, "step1")))
                    .step2Json(item.get("step2") instanceof Map<?, ?> ? JSON.toJSONString(item.get("step2")) : null)
                    .step3Json(item.get("step3") instanceof Map<?, ?> ? JSON.toJSONString(item.get("step3")) : null)
                    .status("WAIT_SOURCE").build());
        }

        if ("DIRECT".equals(sourceReadyMode)) {
            markCurrentWaveReadyDirect(plan);
            afterWarehouseReadyBatch(plan);
        } else {
            // prepareMaterials 与 AGV 预定位并行：仓储接受首件请求后，只下发独立的
            // AgvMove 到仓储站点；真正的 step1/LOAD 仍严格等待 READY。
            afterWarehouseReady(plan);
        }
        log.info("[AgvWarehouse] 启动搬运编排 taskId={} count={} waveSize={} sourceReadyMode={}", taskId,
                transferItems.size(), waveSize, sourceReadyMode);
    }

    /**
     * DIRECT 模式：源库位上的物料本就就绪（如机台间转运），跳过仓储 prepareMaterials/slot-ready
     * 这一环，把当前波次的 WAIT_SOURCE 物料直接标记为 READY。
     */
    private void markCurrentWaveReadyDirect(AgvLoadPlanDO plan) {
        List<AgvLoadItemDO> waveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), plan.getCurrentWave()))
                .filter(item -> "WAIT_SOURCE".equals(item.getStatus())).toList();
        LocalDateTime now = LocalDateTime.now();
        for (AgvLoadItemDO item : waveItems) {
            item.setStatus("READY");
            item.setReadyAt(now);
            itemMapper.updateById(item);
        }
        log.info("[AgvWarehouse] DIRECT模式波次就绪 taskId={} wave={} count={}", plan.getTaskId(),
                plan.getCurrentWave(), waveItems.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerIfEnabled(String taskId, List<Map<String, Object>> transferItems,
            Map<String, Object> inputParams) {
        if (!bool(inputParams.get("multiLoadEnabled")))
            return;
        if (planMapper.selectByTaskId(taskId) != null)
            return;
        if (!bool(inputParams.get("waitSignalEnabled"))) {
            throw exception(AGV_LOAD_PLAN_INVALID, "multiLoadEnabled=true时waitSignalEnabled必须为true");
        }
        if (transferItems == null || transferItems.isEmpty()) {
            throw exception(AGV_LOAD_PLAN_INVALID, "transferItems不能为空");
        }

        int waveSize = positiveInt(inputParams.get("sourceWindowCapacity"), 2);
        int agvCapacity = positiveInt(inputParams.get("agvCapacity"), 8);
        if (transferItems.size() > agvCapacity) {
            throw exception(AGV_LOAD_PLAN_INVALID, "单车物料数" + transferItems.size() + "超过AGV容量" + agvCapacity);
        }
        int firstWaveCount = Math.min(waveSize, transferItems.size());
        Set<String> firstWaveSources = new HashSet<>();

        AgvLoadPlanDO plan = AgvLoadPlanDO.builder().taskId(taskId)
                .deviceId(string(inputParams.get("agvDeviceId"), "AGV-01"))
                .loadStation(string(inputParams.get("loadStation"), null)).expectedCount(transferItems.size())
                .loadedCount(0).waveSize(waveSize).currentWave(1)
                .allowPartialLoad(bool(inputParams.get("allowPartialLoad"))).agvWaiting(false).status("LOADING")
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
            itemMapper.insert(AgvLoadItemDO.builder().taskId(taskId).sequenceNo(i + 1).sourceSlotId(sourceSlotId)
                    .agvSlotId(required(item, "agvSlotId")).targetSlotId(required(item, "targetSlotId"))
                    .instanceId(instanceId).status(status).dispatchedAt(dispatchedAt).build());
        }
        log.info("[AgvLoadPlan] 创建多轮装载计划 taskId={} count={} waveSize={}", taskId, transferItems.size(),
                waveSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> markSlotReady(WarehouseCallbackReqVO req) {
        // 独立仓储请求：transferTaskId 为空，或 requestId/transferTaskId 对不上任何补料计划，
        // 不报错，按 from/to 直接解绑/占用库位并回写物料 slot_id。
        if (!org.springframework.util.StringUtils.hasText(req.getTransferTaskId()) || (
                planMapper.selectByTaskId(req.getTransferTaskId()) == null
                        && itemMapper.selectByReadyRequestId(req.getRequestId()) == null)) {
            return applyStandaloneWarehouseMove(req);
        }

        AgvLoadItemDO duplicated = itemMapper.selectByReadyRequestId(req.getRequestId());
        if (duplicated != null) {
            if (!Objects.equals(duplicated.getTaskId(), req.getTransferTaskId()) || !Objects.equals(
                    duplicated.getSourceSlotId(), req.getToLocation())) {
                throw exception(SLOT_READY_CONFLICT, "requestId已被其它任务或库位使用");
            }
            AgvLoadPlanDO plan = planMapper.selectByTaskId(req.getTransferTaskId());
            if (plan == null)
                throw exception(AGV_LOAD_PLAN_NOT_EXISTS, req.getTransferTaskId());
            return result(plan, duplicated, false, true);
        }

        AgvLoadPlanDO plan = requireActivePlanForWarehouseReady(req.getTransferTaskId());
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
                stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null, false,
                        Map.of("requestId", req.getRequestId()),
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
        if (slot == null)
            throw exception(SLOT_INFO_NOT_EXISTS);
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
        } else if (!"OCCUPIED".equals(slot.getStatus()) || !Objects.equals(slot.getOccupiedBy(), req.getMaterialId())) {
            throw exception(SLOT_READY_CONFLICT, req.getToLocation() + "当前由" + slot.getOccupiedBy() + "占用");
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
        log.info("[AgvLoadPlan] 源库位就绪 taskId={} slotId={} instanceId={} 异步触发下一步", req.getTransferTaskId(),
                req.getToLocation(), req.getMaterialId());
        return result(plan, item, false, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String taskId, String agvStation) {
        AgvLoadPlanDO probe = planMapper.selectByCurrentAgvTaskId(taskId);
        // 关键排查点：waitSignal没被门禁分支消费掉、掉进了通用WAIT节点兜底逻辑时，看这一行
        // 就知道是probe没查到计划、还是orchestrationNodeId为空、还是status不是WAIT_AGV_*。
        log.info(
                "[AgvWarehouse] handle(waitSignal) probe查询结果 taskId={} agvStation={} probe={} orchestrationNodeId={} status={}",
                taskId, agvStation, probe == null ? "null" : probe.getTaskId(),
                probe == null ? null : probe.getOrchestrationNodeId(), probe == null ? null : probe.getStatus());
        // 兜底重试一次：真实生产遇到过AGV执行startTask后极快回调waitSignal的情况，不排除
        // 跟我们自己"下发指令→写WAIT_AGV_*状态"这个事务提交之间存在极窄的可见性缝隙
        // （具体机制还在排查，靠上面这行日志下次实锤）。这里先加一次短暂重试兜底，
        // 避免真遇到这个窗口时直接判定"不是门禁子任务"、走错兜底分支导致门禁/AGV卡住。
        if (probe == null || probe.getOrchestrationNodeId() == null || !probe.getStatus().startsWith("WAIT_AGV_")) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            AgvLoadPlanDO retried = planMapper.selectByCurrentAgvTaskId(taskId);
            log.info(
                    "[AgvWarehouse] handle(waitSignal) probe重试一次 taskId={} probe={} orchestrationNodeId={} status={}",
                    taskId, retried == null ? "null" : retried.getTaskId(),
                    retried == null ? null : retried.getOrchestrationNodeId(),
                    retried == null ? null : retried.getStatus());
            probe = retried;
        }
        if (probe != null && probe.getOrchestrationNodeId() != null && probe.getStatus().startsWith("WAIT_AGV_")) {
            AgvLoadPlanDO subtaskPlan = planMapper.selectByCurrentAgvTaskIdForUpdate(taskId);
            subtaskPlan.setStatus("WAIT_SITE_ACTION");
            subtaskPlan.setLastAgvStation(agvStation);
            planMapper.updateById(subtaskPlan);
            log.info("[AgvWarehouse] AGV进入现场动作等待 taskId={} operation={} station={}", taskId,
                    subtaskPlan.getCurrentAgvOperation(), agvStation);
            // 门禁联动（开门+startAgain）挪到事务提交后异步执行，waitSignal 立即返回给AGV，
            // 不在这次HTTP请求里等开门指令、AGV startAgain这两次出站调用的往返。
            String planTaskId = subtaskPlan.getTaskId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.maybeGateOnDoorOrResumeAsync(planTaskId);
                }
            });
            return true;
        }
        AgvLoadPlanDO existing = planMapper.selectByTaskId(taskId);
        if (existing == null || !"LOADING".equals(existing.getStatus()))
            return false;
        if (existing.getLoadStation() != null && !existing.getLoadStation().equals(agvStation))
            return false;

        AgvLoadPlanDO plan = requireActivePlanForUpdate(taskId);
        completeDispatchedWave(plan);
        plan.setLastAgvStation(agvStation);

        if (plan.getLoadedCount() >= plan.getExpectedCount()) {
            plan.setStatus("LOAD_COMPLETE");
            plan.setAgvWaiting(false);
            planMapper.updateById(plan);
            CallbackResult callback = stepCallbackSpi.callback(taskId, FLOW_WAIT_NODE, null, true,
                    Map.of("signal", "LOAD_COMPLETE", "loadedCount", plan.getLoadedCount(), "agvStation", agvStation),
                    null, null);
            if (!callback.isSuccess()) {
                throw new ServiceException(AGV_LOAD_PLAN_STATE_INVALID).setMessage(
                        "装载完成但流程WAIT节点推进失败: " + callback.getErrorMsg());
            }
            log.info("[AgvLoadPlan] 当前车次装载完成 taskId={} count={}", taskId, plan.getLoadedCount());
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
        if (existing == null || !"WAIT_SITE_ACTION".equals(existing.getStatus()))
            return false;
        AgvLoadPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(agvTaskId);
        sendResumeCommand(plan);
        return true;
    }

    /**
     * 通知AGV继续执行当前挂起的子任务，并把计划状态切回等待该子任务的终态回调。
     */
    private void sendResumeCommand(AgvLoadPlanDO plan) {
        deviceCommandSpi.send("AGV", "startAgain",
                Map.of("taskId", plan.getCurrentAgvTaskId(), "disableSysFieldInjection", true), plan.getTaskId(),
                "resume_" + plan.getCurrentAgvOperation(), null);
        plan.setStatus("WAIT_AGV_" + plan.getCurrentAgvOperation());
        planMapper.updateById(plan);
        log.info("[AgvWarehouse] 现场动作完成，通知AGV继续 taskId={} agvTaskId={} operation={}", plan.getTaskId(),
                plan.getCurrentAgvTaskId(), plan.getCurrentAgvOperation());
    }

    /**
     * 事务提交后异步执行的门禁联动入口（由 handle() 在 waitSignal 分支通过
     * TransactionSynchronization.afterCommit 调度，见该方法调用处的说明）。
     *
     * <p>重新以 FOR UPDATE 读取最新状态，防止跟 {@link #notifyDoorPermission} 那条并发路径
     * 重复触发开门+startAgain：如果等这个异步任务真正执行时，状态已经不是 WAIT_SITE_ACTION了
     * （比如机台许可信号先一步把门打开、放行AGV了），直接跳过。
     */
    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void maybeGateOnDoorOrResumeAsync(String taskId) {
        try {
            AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
            if (plan == null) {
                log.warn("[AgvWarehouse] 异步门禁检查：计划不存在 taskId={}", taskId);
                return;
            }
            if (!"WAIT_SITE_ACTION".equals(plan.getStatus())) {
                log.info("[AgvWarehouse] 异步门禁检查：状态已变化，跳过 taskId={} status={}", taskId, plan.getStatus());
                return;
            }
            maybeGateOnDoorOrResume(plan);
        } catch (Exception e) {
            log.error("[AgvWarehouse] 异步门禁检查失败 taskId={}", taskId, e);
        }
    }

    /**
     * 事务提交后异步提前打开合成站门（由最后一次 LOAD 完成后的 afterCommit 调度）。
     * 重新 FOR UPDATE 读取计划，禁止复用 LOAD 线程里的 plan 对象，以免覆盖 MOVE 已写入的字段。
     */
    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void openSynthesisDoorBeforeMoveAsync(String taskId) {
        try {
            AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
            if (plan == null) {
                log.warn("[AgvWarehouse] 异步提前开门：计划不存在 taskId={}", taskId);
                return;
            }
            tryOpenSynthesisDoorBeforeMove(plan);
        } catch (Exception e) {
            log.error("[AgvWarehouse] 异步提前开门失败，保留UNLOAD兜底 taskId={}", taskId, e);
        }
    }

    /**
     * AGV 到达卸货等待点（WAIT_SITE_ACTION）后的门禁联动入口。
     *
     * <p>只在 UNLOAD 阶段、且本车次 doorRequired=true 时才生效；其它情况维持原有行为，
     * 停在 WAIT_SITE_ACTION，需要外部显式调用 resume()。
     */
    private void maybeGateOnDoorOrResume(AgvLoadPlanDO plan) {
        if (!"UNLOAD".equals(plan.getCurrentAgvOperation()) || !Boolean.TRUE.equals(plan.getDoorRequired())) {
            return;
        }
        if (Boolean.TRUE.equals(plan.getDoorPermissionGateRequired()) && !Boolean.TRUE.equals(
                plan.getDoorPermissionReceived())) {
            log.info("[AgvWarehouse] AGV已到卸货点，等待机台开门许可 taskId={} agvTaskId={}", plan.getTaskId(),
                    plan.getCurrentAgvTaskId());
            return;
        }
        resumeUnloadAfterDoorReady(plan);
    }

    /**
     * 同步调用开门指令；同一车次成功一次后持久化标记，后续提前开门、permission回调和
     * UNLOAD waitSignal 并发或重试时不再重复调用设备。
     */
    private boolean openDoorIfNeeded(AgvLoadPlanDO plan) {
        if (Boolean.TRUE.equals(plan.getDoorOpened())) {
            return true;
        }
        String deviceType = string(plan.getDoorDeviceId(), "Feeding-Reaction");
        String commandCode = string(plan.getDoorCommandCode(), "OpenCloseDoor");
        DeviceCommandSpi.Result result = deviceCommandSpi.send(deviceType, commandCode,
                Map.of("op", "open", "disableSysFieldInjection", true), plan.getTaskId(),
                "door_open_" + plan.getTaskId(), null);
        if (!result.isSyncCompleted()) {
            log.warn("[AgvWarehouse] 开门指令未同步完成 taskId={} deviceType={} commandCode={}", plan.getTaskId(),
                    deviceType, commandCode);
            return false;
        }
        plan.setDoorOpened(true);
        planMapper.updateById(plan);
        log.info("[AgvWarehouse] 开门完成并记录幂等状态 taskId={} agvTaskId={} operation={}", plan.getTaskId(),
                plan.getCurrentAgvTaskId(), plan.getCurrentAgvOperation());
        return true;
    }

    /**
     * 确认门已打开后，放行当前正在等待的UNLOAD子任务。
     */
    private void resumeUnloadAfterDoorReady(AgvLoadPlanDO plan) {
        if (!openDoorIfNeeded(plan)) {
            log.warn("[AgvWarehouse] 开门未完成，暂不放行AGV taskId={} agvTaskId={}", plan.getTaskId(),
                    plan.getCurrentAgvTaskId());
            return;
        }
        log.info("[AgvWarehouse] 门已打开，放行AGV继续执行卸货 taskId={} agvTaskId={}", plan.getTaskId(),
                plan.getCurrentAgvTaskId());
        sendResumeCommand(plan);
    }

    /**
     * 机台反馈"可以开门"。若 AGV 已经到达卸货点在等（WAIT_SITE_ACTION + UNLOAD），
     * 立即开门放行；否则只记录信号，等 AGV 后续到达卸货点时直接放行，不用再等。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean notifyDoorPermission(String taskId) {
        AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null)
            return false;
        plan.setDoorPermissionReceived(true);
        planMapper.updateById(plan);
        if ("WAIT_SITE_ACTION".equals(plan.getStatus()) && "UNLOAD".equals(plan.getCurrentAgvOperation())) {
            resumeUnloadAfterDoorReady(plan);
        } else if (isReadyToOpenSynthesisDoorBeforeUnload(plan)) {
            if (openDoorIfNeeded(plan)) {
                log.info("[AgvWarehouse] 收到机台开门许可，已在AGV前往合成站途中提前开门 taskId={} status={}", taskId,
                        plan.getStatus());
            }
        } else {
            log.info("[AgvWarehouse] 收到机台开门许可，记录待用 taskId={} status={}", taskId, plan.getStatus());
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String agvTaskId, String runState, String agvId) {
        AgvLoadPlanDO existing = planMapper.selectByCurrentAgvTaskId(agvTaskId);
        if (existing == null || existing.getOrchestrationNodeId() == null)
            return false;
        if ("1".equals(runState))
            return true;

        AgvLoadPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(agvTaskId);
        String planTaskId = plan.getTaskId();

        if (!"2".equals(runState)) {
            plan.setStatus("FAILED");
            planMapper.updateById(plan);
            queueMapper.cancelByAgvTaskId(agvTaskId);
            // AGV子任务物理失败：这个计划已经死了，之前占的AGV/目标槽位不会再被任何后续步骤释放，
            // 必须在这里主动清掉，否则永久卡住（之前一直没做，是排队机制发现的一个真实老漏洞）。
            releasePlanSlots(plan);
            stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null, false,
                    Map.of("agvTaskId", agvTaskId, "runState", runState), "AGV_TASK_ERROR",
                    "AGV子任务失败 runState=" + runState);
            // 先给AGV回ACK，事务提交后再异步调度：队列里排队的下一条AGV指令，以及可能因为
            // 上面释放槽位而能重试成功的编排排队请求。避免在ACK之前就同步给同一台AGV发新指令
            // （AGV可能还没退出上报本次runState的状态，会拒绝新指令）。
            String deviceId = plan.getDeviceId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.dispatchNextQueuedAsync(deviceId);
                    self.tryDispatchQueuedOrchestrationAsync(deviceId);
                }
            });
            return true;
        }

        queueMapper.markDoneByAgvTaskId(agvTaskId); // 先标记完成，让 AGV "释放"

        // 先给AGV回ACK，事务提交后再异步推进下一阶段（LOAD->MOVE / MOVE->UNLOAD / UNLOAD->完成）。
        // 之前是在这里同步调用 completeLoadAndContinue/sendUnloadTask/completeOrchestration，
        // 相当于taskState()的HTTP响应还没返回给AGV，服务端就已经同步给同一台AGV发出了下一条
        // startTask——如果AGV自己要等它这次上报被ack之后才认为空闲，就会拒绝新指令
        // （典型现象：AGV返回"AGV运行中,无法响应"）。改成事务提交后异步执行，
        // 确保taskState()先把200还给AGV，下一条指令再发。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.advanceAfterTaskStateAsync(planTaskId);
            }
        });
        return true;
    }

    /**
     * 人工强制终止一个卡死的补料计划：管理端手动恢复用，不经过正常的AGV/仓储回调触发。
     *
     * <p>执行内容跟 {@link #handle(String, String, String)} 里AGV子任务物理失败的分支完全
     * 一致（状态置FAILED、取消队列里跟这个计划有关的条目、释放占用的AGV/目标槽位、通知
     * 流程WAIT节点失败），额外补上原分支一直缺失的一步：显式清空
     * currentAgvTaskId/currentAgvOperation（否则这两个字段会一直停留在被强制终止前的值，
     * 误导后续任何重新读取这个taskId的地方，误以为AGV还在忙）。
     *
     * <p>已经是终态（COMPLETED/FAILED）的计划直接跳过，避免重复通知流程引擎。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forceCancel(String taskId, String reason) {
        AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null) {
            log.warn("[AgvLoadPlan] 强制终止：计划不存在 taskId={}", taskId);
            return false;
        }
        if ("COMPLETED".equals(plan.getStatus()) || "FAILED".equals(plan.getStatus())) {
            log.info("[AgvLoadPlan] 强制终止：计划已是终态，跳过 taskId={} status={}", taskId, plan.getStatus());
            return false;
        }

        String deviceId = plan.getDeviceId();
        String effectiveReason = reason == null || reason.isBlank() ? "人工强制终止" : reason;
        if (plan.getCurrentAgvTaskId() != null) {
            queueMapper.cancelByAgvTaskId(plan.getCurrentAgvTaskId());
        }
        releasePlanSlots(plan);

        plan.setStatus("FAILED");
        plan.setCurrentAgvTaskId(null);
        plan.setCurrentAgvOperation(null);
        planMapper.updateById(plan);

        if (plan.getOrchestrationNodeId() != null) {
            stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null, false,
                    Map.of("reason", effectiveReason), "MANUAL_CANCEL", effectiveReason);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.dispatchNextQueuedAsync(deviceId);
                self.tryDispatchQueuedOrchestrationAsync(deviceId);
            }
        });
        log.warn("[AgvLoadPlan] 已人工强制终止计划 taskId={} deviceId={} reason={}", taskId, deviceId, effectiveReason);
        return true;
    }

    /**
     * 事务提交后异步推进AGV分阶段任务的下一步（LOAD完成后MOVE / MOVE完成后UNLOAD /
     * UNLOAD完成后结束+调度队列下一条），确保先ACK给AGV，再发下一条指令。
     */
    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void advanceAfterTaskStateAsync(String taskId) {
        try {
            AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
            if (plan == null) {
                log.warn("[AgvLoadPlan] 异步推进下一阶段：计划不存在 taskId={}", taskId);
                return;
            }
            log.info(
                    "[AgvLoadPlan] advanceAfterTaskStateAsync 加锁读取到计划状态 taskId={} status={} currentAgvOperation={} loadedCount={}",
                    taskId, plan.getStatus(), plan.getCurrentAgvOperation(), plan.getLoadedCount());
            switch (plan.getCurrentAgvOperation()) {
                case "PREPOSITION" -> completePrepositionAndContinue(plan);
                case "LOAD" -> completeLoadAndContinue(plan);   // 内部可能调 sendMoveTask，走队列
                case "MOVE" -> sendUnloadTask(plan);             // 走队列
                case "UNLOAD" -> {
                    completeOrchestration(plan);
                    agvQueueService.dispatchNext(plan.getDeviceId());
                }
                default -> log.warn("[AgvLoadPlan] 异步推进下一阶段：未知AGV子任务类型 taskId={} op={}", taskId,
                        plan.getCurrentAgvOperation());
            }
            log.info("[AgvLoadPlan] 已ACK给AGV后异步推进下一阶段完成 taskId={}", taskId);
        } catch (Exception e) {
            log.error("[AgvLoadPlan] 异步推进下一阶段失败 taskId={}", taskId, e);
        }
    }

    /**
     * 事务提交后异步调度AGV队列里排队的下一条任务（当前子任务失败时用），确保先ACK给AGV。
     */
    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void dispatchNextQueuedAsync(String deviceId) {
        try {
            agvQueueService.dispatchNext(deviceId);
        } catch (Exception e) {
            log.error("[AgvLoadPlan] 异步调度队列下一条任务失败 deviceId={}", deviceId, e);
        }
    }

    /**
     * 兜底轮询：见接口文档。逐计划各自独立事务重试，一个计划失败不影响其它计划。
     */
    @Override
    public void pokeActiveWarehousePlans() {
        List<String> taskIds = planMapper.selectActive().stream().filter(p -> !"DIRECT".equals(p.getSourceReadyMode()))
                .map(AgvLoadPlanDO::getTaskId).toList();
        for (String taskId : taskIds) {
            try {
                self.pokeOneWarehousePlan(taskId);
            } catch (Exception e) {
                log.error("[AgvWarehouse] 兜底轮询触发失败 taskId={}", taskId, e);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void pokeOneWarehousePlan(String taskId) {
        AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null || "DIRECT".equals(plan.getSourceReadyMode()))
            return;
        afterWarehouseReady(plan);
    }

    /**
     * 释放某个计划占用的全部AGV/目标槽位。
     *
     * <p>只在计划彻底失败（AGV子任务物理失败）时调用——正常完成路径的槽位释放各自分散在
     * completeLoadAndContinue/commitTransfer 里，不受影响。clearOccupancy 本身没有前置状态
     * 校验，对RESERVED/OCCUPIED/已经EMPTY的槽位调用都是安全的，不用先查当前状态。
     */
    private void releasePlanSlots(AgvLoadPlanDO plan) {
        for (AgvLoadItemDO item : itemMapper.selectByTaskId(plan.getTaskId())) {
            slotMapper.clearOccupancy(item.getAgvSlotId());
            slotMapper.clearOccupancy(item.getTargetSlotId());
        }
    }

    // ── AGV编排请求排队（lab_agv_orchestrate_queue） ──────────────────────────

    /**
     * 槽位冲突时把原始编排请求存入排队表，幂等（同一executionId不会重复插入）。
     */
    private void enqueueOrchestrateRequest(String planType, String deviceId, String executionId, String nodeId,
            List<Map<String, Object>> items, Map<String, Object> inputParams, String reason) {
        if (orchestrateQueueMapper.selectByExecutionId(executionId) != null)
            return;
        orchestrateQueueMapper.insert(
                AgvOrchestrateQueueDO.builder().deviceId(deviceId).planType(planType).executionId(executionId)
                        .nodeId(nodeId).payload(JSON.toJSONString(Map.of("items", items, "inputParams", inputParams)))
                        .status("PENDING").errorMessage(reason).createTime(LocalDateTime.now()).build());
        log.info("[AgvOrchestrateQueue] 槽位冲突，请求已入队等待重试 planType={} executionId={} reason={}", planType,
                executionId, reason);
    }

    /**
     * 事务提交后异步触发一次编排排队重试，供释放AGV/目标槽位的地方调用。
     */
    private void scheduleOrchestrateQueueRetry(String deviceId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.tryDispatchQueuedOrchestrationAsync(deviceId);
            }
        });
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public void tryDispatchQueuedOrchestrationAsync(String deviceId) {
        AgvOrchestrateQueueDO next = orchestrateQueueMapper.selectOldestPending(deviceId);
        if (next == null)
            return;
        if (!"LOAD".equals(next.getPlanType())) {
            return; // RETURN计划的排队记录，交给AgvReturnPlanServiceImpl自己的触发处理
        }
        if (!executionSpi.isRunning(next.getExecutionId())) {
            orchestrateQueueMapper.markCancelled(next.getId());
            log.info("[AgvOrchestrateQueue] executionId={} 已不在RUNNING，放弃排队重试", next.getExecutionId());
            return;
        }
        Map<String, Object> payload = JSON.parseObject(next.getPayload(), Map.class);
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        Map<String, Object> inputParams = (Map<String, Object>) payload.get("inputParams");
        try {
            // 必须走 self 代理调用，才能让 attemptStartWarehouseOrchestration 的
            // REQUIRES_NEW 真正生效——直接 this. 调用会被Spring AOP自调用限制吃掉，
            // 半途失败时已经成功的槽位预留不会回滚，会在本方法自己的事务里被错误提交。
            self.attemptStartWarehouseOrchestration(next.getExecutionId(), next.getNodeId(), items, inputParams);
            orchestrateQueueMapper.markDispatched(next.getId());
            log.info("[AgvOrchestrateQueue] 排队重试成功，编排已启动 executionId={}", next.getExecutionId());
        } catch (ServiceException e) {
            if (!AGV_SLOT_UNAVAILABLE.getCode().equals(e.getCode())) {
                // 非槽位冲突（比如目标位未按补料单预留），重试无意义：放弃排队，把编排节点回调失败，
                // 避免节点永远悬空在WAITING状态无人处理
                orchestrateQueueMapper.markCancelled(next.getId());
                stepCallbackSpi.callback(next.getExecutionId(), next.getNodeId(), null, false, Map.of(),
                        "AGV_ORCHESTRATE_FAILED", e.getMessage());
                log.warn("[AgvOrchestrateQueue] executionId={} 排队重试遇到非槽位冲突错误，放弃并回调失败: {}",
                        next.getExecutionId(), e.getMessage());
            } else {
                orchestrateQueueMapper.updateRetryFailure(next.getId(), e.getMessage());
                log.info("[AgvOrchestrateQueue] executionId={} 仍然槽位冲突，继续排队: {}", next.getExecutionId(),
                        e.getMessage());
            }
        } catch (Exception e) {
            log.error("[AgvOrchestrateQueue] 排队重试异常 executionId={}", next.getExecutionId(), e);
        }
    }

    @Override
    public boolean hasPlan(String taskId) {
        return planMapper.selectByTaskId(taskId) != null;
    }

    @Override
    public Map<String, Object> getProgress(String taskId) {
        AgvLoadPlanDO plan = planMapper.selectByTaskId(taskId);
        if (plan == null)
            throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
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
        if (plan == null)
            throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
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
                throw exception(AGV_LOAD_PLAN_STATE_INVALID, "目标库位预留已丢失: " + item.getTargetSlotId());
            }
            item.setStatus("COMPLETED");
            itemMapper.updateById(item);
        }
        plan.setStatus("COMPLETED");
        planMapper.updateById(plan);
        // AGV槽位在这里才真正释放（补料流程里唯一的释放点），事务提交后触发一次排队重试
        scheduleOrchestrateQueueRetry(plan.getDeviceId());
    }

    /**
     * DIRECT/整波模式专用：波内按序请求下一件（WAREHOUSE逐件模式改走 {@link #refillWarehouseWindow}）。
     */
    private void requestNextWarehouseItemInWave(AgvLoadPlanDO plan) {
        AgvLoadItemDO next = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), plan.getCurrentWave()))
                .filter(item -> "WAIT_SOURCE".equals(item.getStatus()))
                .filter(item -> item.getWarehouseRequestId() == null).findFirst().orElse(null);
        if (next == null)
            return;
        doRequestWarehouseItem(plan, next);
    }

    /**
     * DIRECT/整波模式专用：当前波已经发给AGV去抓取了，提前把下一波第一件的备料请求发给
     * 仓储，让"AGV抓当前波"和"仓储备下一波"并行，不用等这一波AGV实际抓完(taskState回调)
     * 才去问仓储要。WAREHOUSE逐件模式不需要这个方法，直接靠 {@link #refillWarehouseWindow}
     * 的并发窗口自然实现同样的并行效果。
     *
     * <p>只往前预取一波：如果下一波已经有物料在准备中或已经就绪（说明预取过了，或者是
     * 更早的一次调用已经处理过），直接跳过，不重复请求——仓储侧通常只有单机械臂，
     * 同一时间只能处理一个备料请求。
     */
    private void prefetchNextWaveBatch(AgvLoadPlanDO plan) {
        int nextWave = plan.getCurrentWave() + 1;
        List<AgvLoadItemDO> items = itemMapper.selectByTaskId(plan.getTaskId());
        boolean alreadyPrefetched = items.stream().filter(item -> Objects.equals(item.getWaveNo(), nextWave))
                .anyMatch(item -> item.getWarehouseRequestId() != null || "READY".equals(item.getStatus()));
        if (alreadyPrefetched)
            return;
        AgvLoadItemDO next = items.stream().filter(item -> Objects.equals(item.getWaveNo(), nextWave))
                .filter(item -> "WAIT_SOURCE".equals(item.getStatus()))
                .min(Comparator.comparing(AgvLoadItemDO::getSequenceNo)).orElse(null);
        if (next == null)
            return; // 没有下一波了，或者是DIRECT模式（不经过仓储备料这一步）
        doRequestWarehouseItem(plan, next);
    }

    private void doRequestWarehouseItem(AgvLoadPlanDO plan, AgvLoadItemDO next) {
        if (slotMapper.reserveIfEmpty(next.getSourceSlotId()) == 0) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, "仓储中转位不可用: " + next.getSourceSlotId());
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
        // 只对 message 含"非空闲"（如 code=500 "移栽模组非空闲"，仓储机械臂正忙、瞬时状态）
        // 做有限次同步重试：每 2 秒一次、最多重试 5 次（共 6 次尝试），见
        // sendWarehousePrepareWithRetry。这种失败是瞬时繁忙而不是真错误，仓储腾出机械臂后
        // 重试就能成功，比直接抛出等事件链/兜底巡检再触发更及时。注意本方法通常在持有
        // plan 行锁（selectByTaskIdForUpdate）的事务里被调用，重试期间锁一直被持有
        // （最坏约10秒），因此重试只限于"非空闲"这一种瞬时繁忙，其它失败仍然立即抛出，
        // 交回调用方（refillWarehouseWindow等）放弃本次，等下一次自然触发（同一计划的其它
        // 物料就绪/被抓走事件，或兜底轮询Job）再试。
        try {
            sendWarehousePrepareWithRetry(plan, next, params);
        } catch (RuntimeException e) {
            // 关键：请求失败时必须把上面已经落库的副作用（源槽位预留、warehouseRequestId标记）
            // 完全撤销，让这件物料恢复成"从没请求过"的干净状态。之前没做这一步的话，
            // warehouseRequestId 会一直是非空值——refillWarehouseWindow 的 warehouseBusy
            // 判断只看"是不是非空"，不区分"请求成功在途"还是"请求失败留下的残留值"，会把这件
            // 物料永远当成"已经在处理中"，之后不管是同计划其它物料触发的自然重试，还是
            // AgvWarehouseWindowWatchdog兜底巡检，都不会再碰它，永久卡死、只能靠人工SQL恢复。
            // 这是一个真实存在但此前没意识到的漏洞（怀疑是本session一直悬而未决的"AGV-01
            // 队列卡死"背后的真正原因），兜底巡检要真正有效，必须先把这一步补上。
            next.setWarehouseRequestId(null);
            itemMapper.updateById(next);
            slotMapper.clearOccupancy(next.getSourceSlotId());
            throw e;
        }
        log.info("[AgvWarehouse] 请求仓储备料成功 taskId={} subRequestId={} from={} to={}", plan.getTaskId(),
                subRequestId, next.getWarehouseSourceLocation(), next.getSourceSlotId());
    }

    /**
     * 发送仓储备料请求（prepareMaterials）。当仓储返回的消息里带"非空闲"（如 code=500
     * "移栽模组非空闲"，机械臂正在处理上一件搬运，属瞬时繁忙）时，sleep 2 秒后重试，
     * 最多重试 5 次（共 6 次尝试）；仍失败才抛出异常，交回调用方现有兜底机制（同计划其它
     * 物料就绪/被抓走事件，或兜底巡检Job）再试。与 AgvReturnPlanServiceImpl 入库侧
     * 的"非空闲"重试策略一致，只是这里重试次数更多。其余错误立即抛出，不在这里耗锁重试。
     */
    private void sendWarehousePrepareWithRetry(AgvLoadPlanDO plan, AgvLoadItemDO next, Map<String, Object> params) {
        final int maxAttempts = 5; // 首次尝试 + 最多5次重试
        int attempt = 0;
        while (true) {
            attempt++;
            DeviceCommandSpi.Result response = deviceCommandSpi.send("WAREHOUSE", "prepareMaterials", params,
                    plan.getTaskId(), "warehouse_prepare_" + next.getSequenceNo(), null);
            if (response.getResponseBody() == null || response.getResponseBody().isBlank()) {
                return; // 与原有逻辑一致：无响应体视为成功
            }
            Integer code = JSON.parseObject(response.getResponseBody()).getInteger("code");
            if (code == null || code == 0 || code == 1005) {
                return; // 成功，或幂等已处理
            }
            String message = JSON.parseObject(response.getResponseBody()).getString("message");
            if (message == null) {
                message = JSON.parseObject(response.getResponseBody()).getString("msg");
            }
            if (attempt < maxAttempts && message != null && message.contains("非空闲")) {
                log.info(
                        "[AgvWarehouse] 仓储移栽模组非空闲，2s后重试 taskId={} subRequestId={} attempt={}/{} message={}",
                        plan.getTaskId(), next.getWarehouseRequestId(), attempt, maxAttempts, message);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                            "仓储备料重试被中断 code=" + code + ", message=" + message);
                }
                continue;
            }
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, "仓储拒绝备料 code=" + code + ", message=" + message);
        }
    }

    /**
     * 物料状态变化时的统一入口（新建计划、仓储备好一件、AGV抓完一件都会调到这里）。
     *
     * <p>WAREHOUSE 模式（仓储备料，默认）：首件 prepareMaterials 被接受后，独立下发
     * PREPOSITION/AgvMove 到仓储站；真正 step1 仍等待 READY。AGV 永远一次只抓一件，必须收到
     * 当前件 taskState=2 后才抓下一件。仓储按 sourceWindowCapacity 提前备料，使两侧并行，
     * 但绝不会一次收到打包了多件物料的抓取指令。见 {@link #tryDispatchReadyItem}
     * / {@link #refillWarehouseWindow}。
     *
     * <p>DIRECT 模式（机台转运等，源头物料本就就绪、没有仓储备料瓶颈）维持原有"整波一次性
     * 打包发给AGV"的行为，见 {@link #afterWarehouseReadyBatch}——这种场景下 waveSize 通常
     * 直接等于整车容量，一趟车本来就该一次性抓完，不受这次改造影响。
     */
    private boolean afterWarehouseReady(AgvLoadPlanDO plan) {
        if ("DIRECT".equals(plan.getSourceReadyMode())) {
            return afterWarehouseReadyBatch(plan);
        }
        boolean requested = refillWarehouseWindow(plan);
        if (shouldStartPreposition(plan)) {
            sendPrepositionTask(plan);
            return true;
        }
        boolean dispatched = tryDispatchReadyItem(plan);
        return dispatched || requested;
    }

    /**
     * 首件 prepareMaterials 已被仓储接受且 AGV 空闲时，启动独立预定位任务。
     */
    private boolean shouldStartPreposition(AgvLoadPlanDO plan) {
        if (plan.getLoadedCount() != 0 || plan.getCurrentAgvOperation() != null || !"WAIT_WAREHOUSE".equals(
                plan.getStatus())) {
            return false;
        }
        return itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .anyMatch(item -> item.getWarehouseRequestId() != null);
    }

    private void sendPrepositionTask(AgvLoadPlanDO plan) {
        String agvTaskId = plan.getTaskId() + "-PREPOSITION";
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("taskType", plan.getTaskType());
        command.put("plateType", plan.getPlateType());
        command.put("step2", Map.of("taskName", "AgvMove", "agvStationName", plan.getLoadStation()));
        sendAndEnqueue(plan, agvTaskId, "PREPOSITION",
                Map.of("taskId", agvTaskId, "agvCmdList", List.of(command), "disableSysFieldInjection", true));
    }

    /**
     * AGV空闲时，把序号最小的一件已备好(READY)物料单独发给AGV抓取。
     */
    private boolean tryDispatchReadyItem(AgvLoadPlanDO plan) {
        if (plan.getCurrentAgvOperation() != null) {
            log.info(
                    "[AgvLoadPlan] tryDispatchReadyItem: AGV有子任务在途，跳过 taskId={} currentAgvOperation={} currentAgvTaskId={}",
                    plan.getTaskId(), plan.getCurrentAgvOperation(), plan.getCurrentAgvTaskId());
            return false; // 有子任务在途，不打断，等它taskState=2
        }
        List<AgvLoadItemDO> items = itemMapper.selectByTaskId(plan.getTaskId());
        AgvLoadItemDO readyItem = items.stream().filter(item -> "READY".equals(item.getStatus()))
                .min(Comparator.comparing(AgvLoadItemDO::getSequenceNo)).orElse(null);
        if (readyItem == null) {
            log.info("[AgvLoadPlan] tryDispatchReadyItem: 没有READY物料可派发 taskId={} 当前各物料状态={}",
                    plan.getTaskId(),
                    items.stream().map(i -> i.getSequenceNo() + ":" + i.getStatus()).collect(Collectors.joining(",")));
            return false;
        }
        log.info("[AgvLoadPlan] tryDispatchReadyItem: 派发物料给AGV taskId={} sequenceNo={} sourceSlotId={}",
                plan.getTaskId(), readyItem.getSequenceNo(), readyItem.getSourceSlotId());
        sendLoadTaskForItem(plan, readyItem);
        return true;
    }

    /**
     * 按 sourceWindowCapacity（复用 {@code waveSize} 字段承载，语义是"仓储备料并发窗口"，
     * 跟AGV一次抓几个完全脱钩）控制的窗口，向仓储请求下一个还没请求过的物料。
     * 同一时刻最多允许这么多物料处于"已经在仓储那边、还没被AGV确认抓走"的状态。
     */
    private boolean refillWarehouseWindow(AgvLoadPlanDO plan) {
        List<AgvLoadItemDO> items = itemMapper.selectByTaskId(plan.getTaskId());

        // 仓储机械臂同一时刻只能处理一件的搬运（实测：备料期间再发第二条 prepareMaterials
        // 会被拒绝 code=500 "移栽模组非空闲"），必须等这一件的READY回调真正到了才能发下一件
        // ——这跟AGV"必须收到taskState=2才能发下一条startTask"是同一个物理约束，不能因为
        // 我们自己定义了一个"并发窗口"就假装仓储那边能并发处理多个备料请求。
        boolean warehouseBusy = items.stream()
                .anyMatch(item -> "WAIT_SOURCE".equals(item.getStatus()) && item.getWarehouseRequestId() != null);
        if (warehouseBusy)
            return false;

        // sourceWindowCapacity（复用 waveSize 字段）控制的是另一件事：已经备好、占着物理
        // 中转位、等AGV来抓的物料最多能有几件同时存在（对应真实的中转位数量，比如2个）。
        // 这跟上面"仓储请求本身必须串行"是两个不同维度的约束：请求永远一件一件发，但已经
        // 完成备料、还没被AGV抓走的物料可以同时占着多个中转位——这才是"仓储备下一件"和
        // "AGV抓当前件"能并行的真正原因（下一件的请求发出后，AGV继续抓当前件，两者互不阻塞；
        // 但在下一件的请求被仓储确认完成之前，不会再发第三件的请求）。
        int capacity = plan.getWaveSize();
        long occupyingSlot = items.stream()
                .filter(item -> "READY".equals(item.getStatus()) || "DISPATCHED".equals(item.getStatus())).count();
        if (occupyingSlot >= capacity)
            return false;

        AgvLoadItemDO next = items.stream().filter(item -> "WAIT_SOURCE".equals(item.getStatus()))
                .filter(item -> item.getWarehouseRequestId() == null)
                .min(Comparator.comparing(AgvLoadItemDO::getSequenceNo)).orElse(null);
        if (next == null)
            return false;
        try {
            doRequestWarehouseItem(plan, next);
            return true;
        } catch (Exception e) {
            log.warn(
                    "[AgvWarehouse] 请求仓储备料失败，等下一次触发（比如这一件的READY回调，或AGV抓完当前件）再重试 taskId={} slotId={}",
                    plan.getTaskId(), next.getSourceSlotId(), e);
            return false;
        }
    }

    /**
     * WAREHOUSE 模式下单件抓取：一条 agvCmdList 永远只包含这一件的 step1。
     */
    @SuppressWarnings("unchecked")
    private void sendLoadTaskForItem(AgvLoadPlanDO plan, AgvLoadItemDO item) {
        String agvTaskId = plan.getTaskId() + "-LOAD-" + item.getSequenceNo();
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("taskType", plan.getTaskType());
        command.put("plateType", resolvePlateType(plan, item));
        command.put("step1", JSON.parseObject(item.getStep1Json(), Map.class));
        item.setStatus("DISPATCHED");
        item.setDispatchedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        sendAndEnqueue(plan, agvTaskId, "LOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", List.of(command), "disableSysFieldInjection", true));
    }

    /**
     * DIRECT 模式专用：维持原有"整波所有物料一起打包进一条agvCmdList"的行为。
     */
    private boolean afterWarehouseReadyBatch(AgvLoadPlanDO plan) {
        List<AgvLoadItemDO> waveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), plan.getCurrentWave())).toList();
        long ready = waveItems.stream().filter(item -> "READY".equals(item.getStatus())).count();
        if (ready < waveItems.size()) {
            requestNextWarehouseItemInWave(plan); // 单机械臂：收到一个回调后才请求下一个
            return false;
        }
        sendLoadTaskBatch(plan, waveItems);
        // 本波已下发给AGV抓取，若还有下一波，立刻并行请求仓储备下一波第一件，不等AGV把
        // 这一波实际抓完(taskState回调)才去问仓储要。DIRECT模式一般totalWaves=1用不到这步，
        // 保留是为了兼容极少数会分多波的DIRECT场景。
        try {
            prefetchNextWaveBatch(plan);
        } catch (Exception e) {
            log.warn(
                    "[AgvWarehouse] 预取下一波备料失败(整波模式)，不影响当前波AGV抓取，AGV抓完当前波后按原逻辑重试 taskId={}",
                    plan.getTaskId(), e);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void sendLoadTaskBatch(AgvLoadPlanDO plan, List<AgvLoadItemDO> waveItems) {
        String agvTaskId = plan.getTaskId() + "-LOAD-" + plan.getCurrentWave();
        List<Map<String, Object>> commands = new ArrayList<>();
        for (AgvLoadItemDO item : waveItems) {
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("taskType", plan.getTaskType());
            command.put("plateType", resolvePlateType(plan, item));
            command.put("step1", JSON.parseObject(item.getStep1Json(), Map.class));
            commands.add(command);
            item.setStatus("DISPATCHED");
            item.setDispatchedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        sendAndEnqueue(plan, agvTaskId, "LOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", commands, "disableSysFieldInjection", true));
    }

    /**
     * 磁子(MSB8_T)容器类型需要用磁子专用抓取方式(plateType=1)，其它类型用整趟默认值。
     */
    private static final String MAGNETIC_CONTAINER_TYPE = "MSB8_T";
    private static final String MAGNETIC_PLATE_TYPE = "1";

    /**
     * 按物料自己的 containerType 决定这一件的 plateType（抓取方式），不是整趟车共用一个值。
     *
     * <p>之前的bug：plateType 只在计划(plan)级别存了一份，同一趟车里不管有几种物料类型，
     * 实际发给AGV的抓取方式永远是同一个值——磁子和非磁子混一趟车时，非磁子的物料也会被
     * 错误地用磁子抓取方式（或反之，取决于最后下发的是哪一项）。现在改成每次下发指令时，
     * 按这一件物料自己的 containerType 现算，磁子用专用值，其它用整趟的默认 plateType。
     */
    private String resolvePlateType(AgvLoadPlanDO plan, AgvLoadItemDO item) {
        if (MAGNETIC_CONTAINER_TYPE.equals(item.getContainerType())) {
            return MAGNETIC_PLATE_TYPE;
        }
        return plan.getPlateType();
    }

    /**
     * AGV 到达仓储站点后释放预定位子任务；若物料已经 READY，立即沿用原有 LOAD 调度，
     * 否则保持在仓储站点等待仓储回调。
     */
    private void completePrepositionAndContinue(AgvLoadPlanDO plan) {
        plan.setCurrentAgvTaskId(null);
        plan.setCurrentAgvOperation(null);
        if ("FAILED".equals(plan.getStatus())) {
            planMapper.updateById(plan);
            agvQueueService.dispatchNext(plan.getDeviceId());
            return;
        }
        plan.setStatus("WAIT_WAREHOUSE_PREPOSITIONED");
        planMapper.updateById(plan);
        afterWarehouseReady(plan);
    }

    /**
     * taskState=2 确认一次LOAD子任务完成后的推进入口，按模式分派。
     */
    private void completeLoadAndContinue(AgvLoadPlanDO plan) {
        if ("DIRECT".equals(plan.getSourceReadyMode())) {
            completeLoadAndContinueBatch(plan);
        } else {
            completeLoadAndContinueItem(plan);
        }
    }

    /**
     * WAREHOUSE 模式：这次taskState=2只对应"正在被AGV抓取的那一件"（不是一整波），标记它
     * LOADED、释放它的源库位、占用AGV库位；如果还有物料没抓完，检查有没有已经被仓储备好
     * 的下一件可以立刻抓（大概率有，因为仓储在AGV抓这一件的同时就已经在并行备下一件了），
     * 同时继续按窗口补仓储请求；全部抓完则进入 MOVE 阶段。
     */
    private void completeLoadAndContinueItem(AgvLoadPlanDO plan) {
        AgvLoadItemDO item = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(i -> "DISPATCHED".equals(i.getStatus())).findFirst()
                .orElseThrow(() -> exception(AGV_LOAD_PLAN_STATE_INVALID, "找不到正在抓取的物料"));
        slotMapper.clearOccupancy(item.getSourceSlotId());
        if (slotMapper.occupyReserved(item.getAgvSlotId(), item.getInstanceId()) == 0) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, "AGV库位预留已丢失: " + item.getAgvSlotId());
        }
        item.setStatus("LOADED");
        item.setLoadedAt(LocalDateTime.now());
        itemMapper.updateById(item);

        plan.setLoadedCount(plan.getLoadedCount() + 1);
        plan.setCurrentAgvTaskId(null);
        plan.setCurrentAgvOperation(null);
        log.info(
                "[AgvLoadPlan] completeLoadAndContinueItem: 物料{}已确认抓取完成，清空currentAgvOperation taskId={} loadedCount={}/{}",
                item.getSequenceNo(), plan.getTaskId(), plan.getLoadedCount(), plan.getExpectedCount());
        if (plan.getLoadedCount() < plan.getExpectedCount()) {
            plan.setStatus("WAIT_WAREHOUSE");
            planMapper.updateById(plan);
            afterWarehouseReady(plan); // 立刻检查是否已有备好的下一件可以马上抓，同时继续补窗口
        } else {
            planMapper.updateById(plan);
            sendMoveTask(plan);
            scheduleOpenSynthesisDoorAfterCommit(plan.getTaskId());
        }
    }

    /**
     * DIRECT 模式：维持原有"整波所有物料一起确认完成"的行为。
     */
    private void completeLoadAndContinueBatch(AgvLoadPlanDO plan) {
        List<AgvLoadItemDO> waveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), plan.getCurrentWave()))
                .filter(item -> "DISPATCHED".equals(item.getStatus())).toList();
        for (AgvLoadItemDO item : waveItems) {
            slotMapper.clearOccupancy(item.getSourceSlotId());
            if (slotMapper.occupyReserved(item.getAgvSlotId(), item.getInstanceId()) == 0) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID, "AGV库位预留已丢失: " + item.getAgvSlotId());
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
            markCurrentWaveReadyDirect(plan);
            afterWarehouseReadyBatch(plan);
        } else {
            planMapper.updateById(plan);
            sendMoveTask(plan);
            scheduleOpenSynthesisDoorAfterCommit(plan.getTaskId());
        }
    }

    /**
     * 当前推进事务提交后再异步提前开门，避免占用线程空等行锁，也不挡住 MOVE。
     */
    private void scheduleOpenSynthesisDoorAfterCommit(String taskId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.openSynthesisDoorBeforeMoveAsync(taskId);
            }
        });
    }

    /**
     * 装载完成后尝试提前打开合成站门。失败或尚未收到机台许可均不阻断MOVE，
     * 后续permission回调或UNLOAD waitSignal仍会重试。
     */
    private void tryOpenSynthesisDoorBeforeMove(AgvLoadPlanDO plan) {
        if (!isReadyToOpenSynthesisDoorBeforeUnload(plan)) {
            return;
        }
        if (!openDoorIfNeeded(plan)) {
            log.warn("[AgvWarehouse] LOAD完成后提前打开合成站门失败，MOVE已下发并保留UNLOAD兜底 taskId={}",
                    plan.getTaskId());
        }
    }

    /**
     * 当前车次是否已经装载完毕，且目标是需要开门的合成站并满足机台许可条件。
     */
    private boolean isReadyToOpenSynthesisDoorBeforeUnload(AgvLoadPlanDO plan) {
        if (!Boolean.TRUE.equals(plan.getDoorRequired()) || plan.getLoadedCount() == null
                || plan.getExpectedCount() == null || plan.getLoadedCount() < plan.getExpectedCount()
                || !isSynthesisStationDestination(plan)) {
            return false;
        }
        return !Boolean.TRUE.equals(plan.getDoorPermissionGateRequired()) || Boolean.TRUE.equals(
                plan.getDoorPermissionReceived());
    }

    /**
     * step3工作流编码 AGV_STX 表示本车次最终卸货到手套箱合成站。
     */
    private boolean isSynthesisStationDestination(AgvLoadPlanDO plan) {
        return itemMapper.selectByTaskId(plan.getTaskId()).stream().map(AgvLoadItemDO::getStep3Json)
                .filter(Objects::nonNull).map(JSON::parseObject)
                .anyMatch(step3 -> "AGV_STX".equals(step3.getString("taskName")));
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
            if (item.getStep3Json() == null)
                continue;
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("taskType", plan.getTaskType());
            command.put("plateType", resolvePlateType(plan, item));
            command.put("step3", JSON.parseObject(item.getStep3Json(), Map.class));
            commands.add(command);
        }
        if (commands.isEmpty())
            throw exception(AGV_LOAD_PLAN_INVALID, "step3不能为空");
        sendAndEnqueue(plan, agvTaskId, "UNLOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", commands, "disableSysFieldInjection", true));
    }

    private void completeOrchestration(AgvLoadPlanDO plan) {
        plan.setStatus("WAIT_FLOW_COMMIT");
        planMapper.updateById(plan);
        CallbackResult callback = stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null, true,
                Map.of("loadedCount", plan.getLoadedCount(), "warehouseOrchestration", true), null, null);
        if (!callback.isSuccess()) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, "流程编排节点推进失败: " + callback.getErrorMsg());
        }
    }

    private void completeDispatchedWave(AgvLoadPlanDO plan) {
        List<AgvLoadItemDO> dispatched = itemMapper.selectByTaskIdAndStatus(plan.getTaskId(), "DISPATCHED");
        if (dispatched.isEmpty())
            return;
        for (AgvLoadItemDO item : dispatched) {
            slotMapper.clearOccupancy(item.getSourceSlotId());
            if (slotMapper.occupyReserved(item.getAgvSlotId(), item.getInstanceId()) == 0) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID, "AGV库位预留已丢失: " + item.getAgvSlotId());
            }
            item.setStatus("LOADED");
            item.setLoadedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        plan.setLoadedCount(plan.getLoadedCount() + dispatched.size());
        planMapper.updateById(plan);
    }

    private boolean dispatchNextWaveIfReady(AgvLoadPlanDO plan) {
        if (!Boolean.TRUE.equals(plan.getAgvWaiting()))
            return false;
        int remaining = plan.getExpectedCount() - plan.getLoadedCount();
        int expected = Math.min(plan.getWaveSize(), remaining);
        List<AgvLoadItemDO> ready = itemMapper.selectByTaskIdAndStatus(plan.getTaskId(), "READY");
        if (ready.isEmpty())
            return false;
        if (!Boolean.TRUE.equals(plan.getAllowPartialLoad()) && ready.size() < expected)
            return false;

        int dispatchCount = Boolean.TRUE.equals(plan.getAllowPartialLoad()) ?
                Math.min(expected, ready.size()) :
                expected;
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

        deviceCommandSpi.send("AGV", "startAgain", Map.of("taskId", plan.getTaskId(), "disableSysFieldInjection", true),
                plan.getTaskId(), "agv_load_wave_" + nextWave, null);
        log.info("[AgvLoadPlan] 第{}波就绪，通知AGV继续 taskId={} count={}", nextWave, plan.getTaskId(), dispatchCount);
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
            // 必须用 ForUpdate 锁行：这个方法和 advanceAfterTaskStateAsync（AGV taskState=2
            // 触发的另一条唤醒链路）都会并发读改plan（currentAgvOperation等字段），两边都要
            // 通过同一把行锁互相排队，否则一边刚提交、另一边几乎同时用不加锁的普通select去读，
            // 理论上仍可能拿到旧快照，导致明明AGV已经空出来了，却因为读到"还在抓取中"的
            // 过期状态而漏发下一条指令——之前用的是不加锁的 selectByTaskId，已改成一致的加锁读取。
            AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
            if (plan == null) {
                log.warn("[AgvLoadPlan] 异步触发下一步：计划不存在 taskId={}", taskId);
                return;
            }
            // 关键排查点：这里读到的plan状态就是本次唤醒决策的全部依据，出问题时直接看这一行。
            log.info(
                    "[AgvLoadPlan] triggerNextStepAsync 加锁读取到计划状态 taskId={} status={} currentAgvOperation={} currentAgvTaskId={} loadedCount={}",
                    taskId, plan.getStatus(), plan.getCurrentAgvOperation(), plan.getCurrentAgvTaskId(),
                    plan.getLoadedCount());
            boolean triggered =
                    plan.getOrchestrationNodeId() != null ? afterWarehouseReady(plan) : dispatchNextWaveIfReady(plan);
            log.info("[AgvLoadPlan] 异步触发下一步完成 taskId={} triggered={}", taskId, triggered);
        } catch (Exception e) {
            log.error("[AgvLoadPlan] 异步触发下一步失败 taskId={}", taskId, e);
        }
    }

    private AgvLoadPlanDO requireActivePlanForUpdate(String taskId) {
        AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null)
            throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
        if (!Set.of("LOADING", "WAIT_WAREHOUSE").contains(plan.getStatus())) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, plan.getStatus());
        }
        return plan;
    }

    /**
     * markSlotReady（仓储备好一件物料的回调）专用的"计划是否仍处于收集物料阶段"校验，
     * 比 {@link #requireActivePlanForUpdate} 更宽松。
     *
     * <p>WAREHOUSE 逐件模式下，AGV抓当前件和仓储备下一件是并行的（见 tryDispatchReadyItem/
     * refillWarehouseWindow）：物料A的ready回调完全可能在"AGV正在抓物料B"这段时间窗口内到达，
     * 这时候 plan.status 已经变成 QUEUED 或 WAIT_AGV_LOAD（AGV忙/正在抓B），不再是
     * LOADING/WAIT_WAREHOUSE——但计划显然还处于合法的"继续收集物料"阶段，不能拒绝，
     * 否则仓储那边已经真实完成的备料动作会被我们报错拒收，物料再也没人来处理。
     *
     * <p>真正应该拒绝的是流程已经彻底离开LOAD阶段的情形（MOVE/UNLOAD/已完成/已失败等），
     * 那些情形下 plan.status 会是别的值（WAIT_AGV_MOVE、WAIT_AGV_UNLOAD、WAIT_SITE_ACTION、
     * WAIT_FLOW_COMMIT、COMPLETED、FAILED……），不在下面这个白名单里，依然会被拒绝。
     *
     * <p>{@link #requireActivePlanForUpdate} 本身保持不变，继续供 waitSignal 那条legacy
     * 多波流程（{@code registerIfEnabled}/{@code handle(taskId, agvStation)}）使用，
     * 那条路径不涉及这次的逐件并行改造，没有理由放宽。
     */
    private AgvLoadPlanDO requireActivePlanForWarehouseReady(String taskId) {
        AgvLoadPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null)
            throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
        if (!Set.of("LOADING", "WAIT_WAREHOUSE", "WAIT_WAREHOUSE_PREPOSITIONED", "QUEUED", "WAIT_AGV_PREPOSITION",
                "WAIT_AGV_LOAD").contains(plan.getStatus())) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, plan.getStatus());
        }
        return plan;
    }

    private Map<String, Object> result(AgvLoadPlanDO plan, AgvLoadItemDO item, boolean triggered, boolean duplicated) {
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
        if (value == null)
            return defaultValue;
        int parsed = Integer.parseInt(String.valueOf(value));
        if (parsed <= 0)
            throw exception(AGV_LOAD_PLAN_INVALID, "数值必须大于0: " + value);
        return parsed;
    }

    private boolean bool(Object value) {
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value));
    }

    private String required(Map<String, Object> map, String key) {
        String value = string(map.get(key), null);
        if (value == null)
            throw exception(AGV_LOAD_PLAN_INVALID, key + "不能为空");
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
        if (value == null || value.toString().isBlank())
            return defaultValue;
        return value.toString();
    }

    /**
     * 入队或立即调度 AGV startTask，并持久化计划状态。
     */
    private void sendAndEnqueue(AgvLoadPlanDO plan, String agvTaskId, String operation, Map<String, Object> params) {
        plan.setCurrentAgvTaskId(agvTaskId);
        plan.setCurrentAgvOperation(operation);
        plan.setStatus(agvQueueService.enqueueOrDispatch(plan, agvTaskId, operation, params));
        planMapper.updateById(plan);
    }

    /**
     * 独立仓储搬运：查不到补料/下料计划时，按 from/to 维护库位占用与物料当前位置。
     *
     * <ul>
     *   <li>{@code fromLocation} — 解绑，库位置 EMPTY</li>
     *   <li>{@code toLocation} — 占用，库位置 OCCUPIED，物料 slot_id 指向此处</li>
     *   <li>库位或物料在本系统不存在时跳过该项，不抛错</li>
     * </ul>
     */
    private Map<String, Object> applyStandaloneWarehouseMove(WarehouseCallbackReqVO req) {
        String fromLocation = req.getFromLocation();
        String toLocation = req.getToLocation();
        String materialId = req.getMaterialId();
        boolean fromReleased = false;
        boolean toOccupied = false;
        boolean materialUpdated = false;

        if (req.isSuccess()) {
            if (org.springframework.util.StringUtils.hasText(fromLocation) && !fromLocation.equals(toLocation)) {
                SlotInfoDO fromSlot = slotMapper.selectBySlotId(fromLocation);
                if (fromSlot == null) {
                    log.info("[SlotReady] 独立搬运：from 库位不在本系统，跳过解绑 from={}", fromLocation);
                } else {
                    slotMapper.clearOccupancy(fromLocation);
                    fromReleased = true;
                }
            }

            String zoneCode = null;
            if (org.springframework.util.StringUtils.hasText(toLocation)) {
                SlotInfoDO toSlot = slotMapper.selectBySlotId(toLocation);
                if (toSlot == null) {
                    log.info("[SlotReady] 独立搬运：to 库位不在本系统，跳过占用 to={}", toLocation);
                } else if (!Boolean.TRUE.equals(toSlot.getEnabled()) || "DISABLED".equals(toSlot.getStatus())) {
                    log.warn("[SlotReady] 独立搬运：to 库位不可用，跳过占用 to={} status={}", toLocation,
                            toSlot.getStatus());
                } else {
                    slotMapper.occupyDirect(toLocation, materialId);
                    zoneCode = toSlot.getZoneCode();
                    toOccupied = true;
                }
            }

            if (org.springframework.util.StringUtils.hasText(materialId)) {
                String previousSlotId = materialLocationSpi.getCurrentSlotId(materialId);
                if (org.springframework.util.StringUtils.hasText(previousSlotId) && !previousSlotId.equals(fromLocation)
                        && !previousSlotId.equals(toLocation) && slotMapper.selectBySlotId(previousSlotId) != null) {
                    slotMapper.clearOccupancy(previousSlotId);
                    log.info("[SlotReady] 独立搬运：释放物料原库位 materialId={} previousSlot={}", materialId,
                            previousSlotId);
                }
                String targetSlotId = org.springframework.util.StringUtils.hasText(toLocation) ? toLocation : null;
                materialUpdated = materialLocationSpi.updateCurrentSlot(materialId, targetSlotId, zoneCode);
            }
        }

        log.info(
                "[SlotReady] 独立仓储搬运 requestId={} transferTaskId={} materialId={} from={} to={} status={} fromReleased={} toOccupied={} materialUpdated={}",
                req.getRequestId(), req.getTransferTaskId(), materialId, fromLocation, toLocation, req.getStatus(),
                fromReleased, toOccupied, materialUpdated);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("standalone", true);
        result.put("requestId", req.getRequestId());
        result.put("transferTaskId", req.getTransferTaskId());
        result.put("materialId", materialId);
        result.put("fromLocation", fromLocation);
        result.put("toLocation", toLocation);
        result.put("status", req.getStatus());
        result.put("fromReleased", fromReleased);
        result.put("toOccupied", toOccupied);
        result.put("materialUpdated", materialUpdated);
        return result;
    }
}
