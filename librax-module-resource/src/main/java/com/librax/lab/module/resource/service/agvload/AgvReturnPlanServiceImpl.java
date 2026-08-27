package com.librax.lab.module.resource.service.agvload;

import com.alibaba.fastjson.JSON;
import com.librax.lab.framework.common.exception.ServiceException;
import com.librax.lab.module.device.api.AgvReturnLoadCompleteHandler;
import com.librax.lab.module.device.api.AgvResumeHandler;
import com.librax.lab.module.device.api.AgvTaskStateHandler;
import com.librax.lab.module.device.api.AgvWaitSignalHandler;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.flow.api.pipeline.PipelineExecutionSpi;
import com.librax.lab.module.resource.controller.app.slotinfo.vo.WarehouseCallbackReqVO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvOrchestrateQueueDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnItemDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnPlanDO;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvOrchestrateQueueMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvReturnItemMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvReturnPlanMapper;
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
public class AgvReturnPlanServiceImpl
        implements AgvReturnPlanService, AgvTaskStateHandler, AgvWaitSignalHandler, AgvResumeHandler {

    /**
     * 自注入，用于在事务提交后通过 Spring 代理调用 @Async 方法
     */
    @Lazy
    @Autowired
    private AgvReturnPlanService self;

    private final AgvReturnPlanMapper planMapper;
    private final AgvReturnItemMapper itemMapper;
    private final AgvTaskQueueMapper queueMapper;
    private final AgvOrchestrateQueueMapper orchestrateQueueMapper;
    private final SlotInfoMapper slotMapper;
    private final DeviceCommandSpi deviceCommandSpi;
    private final StepCallbackSpi stepCallbackSpi;
    private final AgvQueueService agvQueueService;
    private final PipelineExecutionSpi executionSpi;
    private final List<AgvReturnLoadCompleteHandler> loadCompleteHandlers;

    // ── AgvReturnPlanService ─────────────────────────────────────────────────

    @Override
    public void startReturnOrchestration(String taskId, String nodeId, List<Map<String, Object>> returnItems,
            Map<String, Object> inputParams) {
        if (planMapper.selectByTaskId(taskId) != null)
            return;
        if (orchestrateQueueMapper.selectByExecutionId(taskId) != null) {
            log.info("[AgvReturn] taskId={} 已在编排排队队列中，忽略重复请求", taskId);
            return;
        }
        try {
            self.attemptStartReturnOrchestration(taskId, nodeId, returnItems, inputParams);
        } catch (ServiceException e) {
            if (!AGV_SLOT_UNAVAILABLE.getCode().equals(e.getCode())) {
                throw e; // 非槽位冲突（参数错误、容量超限等）维持原有硬失败行为，不排队
            }
            enqueueOrchestrateRequest("RETURN", string(inputParams.get("agvDeviceId"), "AGV-01"), taskId, nodeId,
                    returnItems, inputParams, e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void attemptStartReturnOrchestration(String taskId, String nodeId, List<Map<String, Object>> returnItems,
            Map<String, Object> inputParams) {
        if (planMapper.selectByTaskId(taskId) != null)
            return;
        if (returnItems == null || returnItems.isEmpty()) {
            throw exception(AGV_LOAD_PLAN_INVALID, "returnItems不能为空");
        }
        int waveSize = positiveInt(inputParams.get("transitCapacity"), 2);
        int agvCapacity = positiveInt(inputParams.get("agvCapacity"), 8);
        if (returnItems.size() > agvCapacity) {
            throw exception(AGV_LOAD_PLAN_INVALID, "单车物料数" + returnItems.size() + "超过AGV容量" + agvCapacity);
        }
        int totalWaves = (returnItems.size() + waveSize - 1) / waveSize;

        // 预留 AGV 槽位和中转位
        // 同一中转位可能被多个波次复用，只需预留一次
        Set<String> reservedTransitSlots = new java.util.HashSet<>();
        for (Map<String, Object> item : returnItems) {
            String agvSlot = required(item, "agvSlotId");
            String transit = required(item, "transitSlotId");
            if (slotMapper.reserveIfEmpty(agvSlot) == 0) {
                throw exception(AGV_SLOT_UNAVAILABLE, "AGV槽位不可用: " + agvSlot);
            }
            if (!reservedTransitSlots.contains(transit)) {
                if (slotMapper.reserveIfEmpty(transit) == 0) {
                    throw exception(AGV_SLOT_UNAVAILABLE, "中转位不可用: " + transit);
                }
                reservedTransitSlots.add(transit);
            }
        }

        AgvReturnPlanDO plan = AgvReturnPlanDO.builder().taskId(taskId)
                .deviceId(string(inputParams.get("agvDeviceId"), "AGV-01")).expectedCount(returnItems.size())
                .returnedCount(0).waveSize(waveSize).currentWave(1).totalWaves(totalWaves).orchestrationNodeId(nodeId)
                .warehouseCallbackUrl(string(inputParams.get("warehouseCallbackUrl"),
                        "http://localhost:48080/app-api/resource/slot/returned"))
                .taskType(string(inputParams.get("taskType"), "1"))
                .plateType(string(inputParams.get("plateType"), "Plate_5"))
                //                .closeStationDoorAfterLoad(bool(inputParams.get("closeStationDoorAfterLoad")))
                .status("WAIT_AGV_LOAD").build();
        planMapper.insert(plan);

        List<AgvReturnItemDO> items = new ArrayList<>();
        for (int i = 0; i < returnItems.size(); i++) {
            Map<String, Object> item = returnItems.get(i);
            AgvReturnItemDO entity = AgvReturnItemDO.builder().taskId(taskId).sequenceNo(i + 1).waveNo(i / waveSize + 1)
                    .sourceSlotId(required(item, "sourceSlotId")).agvSlotId(required(item, "agvSlotId"))
                    .transitSlotId(required(item, "transitSlotId"))
                    .warehouseTargetLocation(required(item, "warehouseTargetLocation"))
                    .instanceId(required(item, "instanceId")).containerType(string(item.get("containerType"), null))
                    .step1Json(JSON.toJSONString(requiredMap(item, "step1")))
                    .step2Json(item.get("step2") instanceof Map<?, ?> ? JSON.toJSONString(item.get("step2")) : null)
                    .step3Json(JSON.toJSONString(requiredMap(item, "step3"))).status("WAIT_LOAD").build();
            itemMapper.insert(entity);
            items.add(entity);
        }

        sendLoadTask(plan, items);
        log.info("[AgvReturn] 启动下料编排 taskId={} count={} waveSize={} totalWaves={}", taskId, returnItems.size(),
                waveSize, totalWaves);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> markSlotReturned(WarehouseCallbackReqVO req) {
        if (!org.springframework.util.StringUtils.hasText(req.getTransferTaskId())) {
            log.info("[SlotReturned] 独立入库 slotId={} instanceId={} status={}", req.getFromLocation(),
                    req.getMaterialId(), req.getStatus());
            return Map.of("standalone", true, "slotId", req.getFromLocation());
        }

        AgvReturnPlanDO plan = planMapper.selectByTaskIdForUpdate(req.getTransferTaskId());
        if (plan == null)
            throw exception(AGV_LOAD_PLAN_NOT_EXISTS, req.getTransferTaskId());

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
        // 中转位在这里释放，是编排排队重试可能等待的资源之一，事务提交后触发一次重试
        scheduleOrchestrateQueueRetry(plan.getDeviceId());

        int wave = item.getWaveNo();

        // 当前波次还有物料等待仓储处理（只检查，不在事务内发起外部调用）
        if (itemMapper.selectNextPendingReturn(plan.getTaskId(), wave) != null) {
            log.info("[AgvReturn] 波次{}继续入库 taskId={} slotId={} 异步触发下一条仓储请求", wave,
                    req.getTransferTaskId(), req.getFromLocation());
            String planTaskId = plan.getTaskId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.triggerNextStepAsync(planTaskId);
                }
            });
            return result(plan, item);
        }

        // 波次全部 RETURNED，检查是否还有下一波
        boolean waveComplete = itemMapper.selectByTaskIdAndWave(req.getTransferTaskId(), wave).stream()
                .allMatch(i -> "RETURNED".equals(i.getStatus()) || "COMPLETED".equals(i.getStatus()));
        if (!waveComplete) {
            return result(plan, item); // 有其他 IN_TRANSIT 项的仓储请求已在途
        }

        plan.setReturnedCount(
                plan.getReturnedCount() + (int) itemMapper.selectByTaskIdAndWave(req.getTransferTaskId(), wave).stream()
                        .filter(i -> "RETURNED".equals(i.getStatus())).count());

        // 注意：下面用的是"这一波"(局部变量 wave，即刚刚全部入库完成的那一波)而不是
        // plan.getCurrentWave()——有了 tryPrefetchNextUnloadWave 之后，AGV可能已经提前把
        // 更晚的波次卸完、plan.currentWave 早就往前推进了，这里如果还用 plan.getCurrentWave()
        // 判断"是否还有下一波"/"是否要重新推进"，会错误地把提前卸完的后续波次也算进"当前波"，
        // 导致流程过早完成回调，或者对已经提前卸过的波次重复下发一次卸料指令。
        boolean triggerNext;
        if (wave < plan.getTotalWaves()) {
            if (plan.getCurrentWave() <= wave) {
                // 下一波还没被提前卸过（比如中转位复用、没条件提前卸）：按原逻辑推进+触发
                plan.setCurrentWave(wave + 1);
                triggerNext = true;
                log.info("[AgvReturn] 波次{}完成，异步触发下一波卸料 taskId={}", wave, req.getTransferTaskId());
            } else {
                // plan.currentWave 已经领先于 wave，说明下一波早被 tryPrefetchNextUnloadWave
                // 提前卸过了，这里只需要落盘 returnedCount，不用再重复推进或触发
                triggerNext = false;
                log.info("[AgvReturn] 波次{}完成，下一波已提前卸料，无需重复触发 taskId={}", wave,
                        req.getTransferTaskId());
            }
        } else {
            // 全部完成：先持久化 WAIT_FLOW_COMMIT，事务提交后再回调流程
            plan.setStatus("WAIT_FLOW_COMMIT");
            triggerNext = true;
            log.info("[AgvReturn] 全部入库完成，异步触发流程回调 taskId={}", req.getTransferTaskId());
        }
        planMapper.updateById(plan);

        if (triggerNext) {
            String planTaskId = plan.getTaskId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.triggerNextStepAsync(planTaskId);
                }
            });
        }
        return result(plan, item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void commitReturn(String taskId) {
        AgvReturnPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null)
            throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
        if (!"WAIT_FLOW_COMMIT".equals(plan.getStatus())) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, plan.getStatus());
        }
        List<AgvReturnItemDO> items = itemMapper.selectByTaskId(taskId);
        for (AgvReturnItemDO item : items) {
            if (!"RETURNED".equals(item.getStatus())) {
                throw exception(AGV_LOAD_PLAN_STATE_INVALID,
                        "物料" + item.getSequenceNo() + "状态=" + item.getStatus());
            }
            // 这里原来有一句"AGV槽位在UNLOAD完成时已清理，此处再次保护"的兜底clearOccupancy，
            // 已删除——这是一个真实出现过的生产bug：commitReturn跑在整条"仓储入库确认"链路
            // 走完之后，时间点可能比afterUnloadComplete晚很多（本例晚了几十秒到几分钟）；
            // clearOccupancy本身不做任何归属校验，只要slotId对得上就无条件把状态打成EMPTY。
            // 如果这段延迟窗口内，同一个AGV槽位码已经被另一个完全无关的新计划（哪怕是LOAD
            // 计划）重新预留（RESERVED），这句"兜底"清理会把那个新计划刚设好的预留状态
            // 直接冲掉，导致对方在AGV完成抓取后 occupyReserved 判断"预留已丢失"而失败。
            // afterUnloadComplete 里的清理已经是唯一且时机正确的释放点（AGV物理卸货那一刻），
            // 这里不需要、也不能再重复清一次。
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
        if (plan == null)
            throw exception(AGV_LOAD_PLAN_NOT_EXISTS, taskId);
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
        if (existing == null || existing.getOrchestrationNodeId() == null)
            return false;
        if ("1".equals(runState))
            return true; // 运行中，不做处理

        AgvReturnPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(agvTaskId);
        String planTaskId = plan.getTaskId();

        if (!"2".equals(runState)) {
            plan.setStatus("FAILED");
            planMapper.updateById(plan);
            queueMapper.cancelByAgvTaskId(agvTaskId);
            // AGV子任务物理失败：这个计划已经死了，之前占的AGV/中转槽位不会再被任何后续步骤释放，
            // 必须在这里主动清掉，否则永久卡住（补料侧同款漏洞，一起补上）。
            releasePlanSlots(plan);
            stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null, false,
                    Map.of("agvTaskId", agvTaskId, "runState", runState), "AGV_TASK_ERROR",
                    "AGV下料子任务失败 runState=" + runState);
            // 先给AGV回ACK，事务提交后再异步调度：队列下一条任务，以及可能因为上面释放槽位
            // 而能重试成功的编排排队请求，跟补料侧 AgvLoadPlanServiceImpl 的处理方式保持一致。
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

        queueMapper.markDoneByAgvTaskId(agvTaskId);

        // 先给AGV回ACK，事务提交后再异步推进下一阶段（LOAD->MOVE / MOVE->UNLOAD / UNLOAD->完成）。
        // 跟 AgvLoadPlanServiceImpl.handle() 同样的坑：这里原先是同步调用
        // afterLoadComplete/sendUnloadWave/afterUnloadComplete，taskState()的HTTP响应还没
        // 返回给AGV，服务端就已经同步给同一台AGV发出了下一条startTask，AGV可能因为还没退出
        // 上报本次runState的状态而拒绝新指令（"AGV运行中,无法响应"）。改成事务提交后异步执行。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.advanceAfterTaskStateAsync(planTaskId);
            }
        });
        return true;
    }

    /**
     * 兜底轮询：见接口文档。逐计划各自独立事务重试，一个计划失败不影响其它计划。
     */
    @Override
    public void pokeActiveWarehousePlans() {
        List<String> taskIds = planMapper.selectActive().stream().map(AgvReturnPlanDO::getTaskId).toList();
        for (String taskId : taskIds) {
            try {
                self.pokeOneWarehousePlan(taskId);
            } catch (Exception e) {
                log.error("[AgvReturn] 仓储窗口兜底轮询触发失败 taskId={}", taskId, e);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void pokeOneWarehousePlan(String taskId) {
        AgvReturnPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null)
            return;
        // requestNextWarehouseReturn 内部靠 selectNextPendingReturn（要求 warehouseRequestId
        // IS NULL）挑要重试的物料：正常情况下（没有卡住的物料，或者当前确实有请求在途）
        // 会安全地找不到东西可做、什么也不发生；只有真的卡住（比如波次第一件请求被拒绝、
        // warehouseRequestId 已经在失败处理里重置为 null）时才会真正重新发一次请求，
        // 不会对正常运行中的计划产生任何副作用。
        //
        // 这里必须本地catch：如果这次巡检重试又失败了（比如仓储还是拒绝），
        // requestNextWarehouseReturn 内部会先把 warehouseRequestId 重置为 null 再往外抛异常；
        // 如果不在这里捕获、任由异常穿透这个 REQUIRES_NEW 事务的代理边界，Spring会把整个
        // 事务连同刚才那个重置写入一起回滚，等于这次巡检不但没修好、还把状态清理的效果也
        // 撤销了——那就真的"帮倒忙"了。捕获后只记录日志，交给下一轮巡检或事件驱动重试。
        try {
            requestNextWarehouseReturn(plan, plan.getCurrentWave());
        } catch (Exception e) {
            log.warn("[AgvReturn] 兜底巡检重试仓储入库请求仍失败 taskId={} wave={}", taskId, plan.getCurrentWave(), e);
        }
    }

    /**
     * 人工强制终止一个卡死的下料计划：管理端手动恢复用，不经过正常的AGV/仓储回调触发。
     *
     * <p>跟 {@link #handle(String, String, String)} 里AGV子任务物理失败的分支一致（状态置
     * FAILED、取消队列条目、释放槽位、通知流程WAIT节点失败），额外清空
     * currentAgvTaskId/currentAgvOperation，跟补料侧 AgvLoadPlanServiceImpl 保持一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forceCancel(String taskId, String reason) {
        AgvReturnPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
        if (plan == null) {
            log.warn("[AgvReturn] 强制终止：计划不存在 taskId={}", taskId);
            return false;
        }
        if ("COMPLETED".equals(plan.getStatus()) || "FAILED".equals(plan.getStatus())) {
            log.info("[AgvReturn] 强制终止：计划已是终态，跳过 taskId={} status={}", taskId, plan.getStatus());
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
        log.warn("[AgvReturn] 已人工强制终止计划 taskId={} deviceId={} reason={}", taskId, deviceId, effectiveReason);
        return true;
    }

    /**
     * 事务提交后异步推进AGV下料分阶段任务的下一步（LOAD完成后MOVE / MOVE完成后开始第一波UNLOAD /
     * 某一波UNLOAD完成后标记IN_TRANSIT+请求仓储+让AGV可承接其它任务），确保先ACK给AGV。
     */
    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void advanceAfterTaskStateAsync(String taskId) {
        try {
            AgvReturnPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
            if (plan == null) {
                log.warn("[AgvReturn] 异步推进下一阶段：计划不存在 taskId={}", taskId);
                return;
            }
            switch (plan.getCurrentAgvOperation()) {
                case "LOAD" -> afterLoadComplete(plan);
                case "MOVE" -> sendUnloadWave(plan);    // 开始第一波卸料
                case "UNLOAD" -> {
                    afterUnloadComplete(plan);          // 标记 IN_TRANSIT，请求仓储
                    agvQueueService.dispatchNext(plan.getDeviceId()); // 让 AGV 可承接其它任务
                }
                default -> log.warn("[AgvReturn] 异步推进下一阶段：未知AGV下料操作 taskId={} op={}", taskId,
                        plan.getCurrentAgvOperation());
            }
            log.info("[AgvReturn] 已ACK给AGV后异步推进下一阶段完成 taskId={}", taskId);
        } catch (Exception e) {
            log.error("[AgvReturn] 异步推进下一阶段失败 taskId={}", taskId, e);
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
            log.error("[AgvReturn] 异步调度队列下一条任务失败 deviceId={}", deviceId, e);
        }
    }

    // ── AgvWaitSignalHandler ─────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String taskId, String agvStation) {
        AgvReturnPlanDO existing = planMapper.selectByCurrentAgvTaskId(taskId);
        if (existing == null || existing.getOrchestrationNodeId() == null || !existing.getStatus()
                .startsWith("WAIT_AGV_"))
            return false;
        AgvReturnPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(taskId);
        plan.setStatus("WAIT_SITE_ACTION");
        plan.setLastAgvStation(agvStation);
        planMapper.updateById(plan);
        log.info("[AgvReturn] AGV进入现场动作等待 taskId={} operation={} station={}", taskId,
                plan.getCurrentAgvOperation(), agvStation);
        return true;
    }

    // ── AgvResumeHandler ─────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(String agvTaskId) {
        AgvReturnPlanDO existing = planMapper.selectByCurrentAgvTaskId(agvTaskId);
        if (existing == null || !"WAIT_SITE_ACTION".equals(existing.getStatus()))
            return false;
        AgvReturnPlanDO plan = planMapper.selectByCurrentAgvTaskIdForUpdate(agvTaskId);
        deviceCommandSpi.send("AGV", "startAgain", Map.of("taskId", agvTaskId, "disableSysFieldInjection", true),
                plan.getTaskId(), "resume_return_" + plan.getCurrentAgvOperation(), null);
        plan.setStatus("WAIT_AGV_" + plan.getCurrentAgvOperation());
        planMapper.updateById(plan);
        log.info("[AgvReturn] 现场动作完成，通知AGV继续 taskId={} operation={}", agvTaskId,
                plan.getCurrentAgvOperation());
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
            cmd.put("plateType", resolvePlateType(plan, item));
            cmd.put("step1", JSON.parseObject(item.getStep1Json(), Map.class));
            commands.add(cmd);
            item.setDispatchedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        sendAndEnqueue(plan, agvTaskId, "LOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", commands, "disableSysFieldInjection", true));
    }

    /**
     * 磁子(MSB8_T)容器类型需要用磁子专用抓取方式(plateType=1)，其它类型用整趟默认值。
     * 跟 AgvLoadPlanServiceImpl.resolvePlateType 是同一条规则——下料(Return)是另一套独立的
     * 计划/物料实体(AgvReturnPlanDO/AgvReturnItemDO)，之前只修了补料/转运那边的
     * AgvLoadPlanServiceImpl，这边下料的 sendLoadTask/sendUnloadWave 还是按整趟单一
     * plan.getPlateType() 下发，同样的"整趟车最后覆盖前面物料抓取方式"的bug在下料这边一直没修。
     */
    private static final String MAGNETIC_CONTAINER_TYPE = "MSB8_T";
    private static final String MAGNETIC_PLATE_TYPE = "1";

    private String resolvePlateType(AgvReturnPlanDO plan, AgvReturnItemDO item) {
        if (MAGNETIC_CONTAINER_TYPE.equals(item.getContainerType())) {
            return MAGNETIC_PLATE_TYPE;
        }
        return plan.getPlateType();
    }

    private void afterLoadComplete(AgvReturnPlanDO plan) {
        List<AgvReturnItemDO> allItems = itemMapper.selectByTaskId(plan.getTaskId());
        for (AgvReturnItemDO item : allItems) {
            slotMapper.clearOccupancy(item.getSourceSlotId()); // 站位库位释放
            item.setStatus("ON_AGV");
            item.setLoadedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        // AGV抓取后通知
        notifyLoadComplete(plan);
        sendMoveTask(plan);
    }

    /**
     * AGV 已完成所有 step1 装载。业务侧通知属于旁路动作，失败不能阻断回仓。
     */
    private void notifyLoadComplete(AgvReturnPlanDO plan) {
        for (AgvReturnLoadCompleteHandler handler : loadCompleteHandlers) {
            try {
                handler.onLoadComplete(plan.getTaskId());
            } catch (Exception e) {
                log.warn("[AgvReturn] AGV装载完成业务通知失败，继续执行后续搬运 taskId={}", plan.getTaskId(), e);
            }
        }
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
                .filter(item -> Objects.equals(item.getWaveNo(), wave) && "ON_AGV".equals(item.getStatus())).toList();
        if (waveItems.isEmpty()) {
            throw exception(AGV_LOAD_PLAN_INVALID, "第" + wave + "波无待卸料物料");
        }
        // 波次 2+ 时中转位已被上一波入库后清空为 EMPTY，需重新预留为 RESERVED
        // 波次 1 时中转位已在 startReturnOrchestration 预留，reserveIfEmpty 返回 0 无副作用
        Set<String> reservedTransits = new java.util.HashSet<>();
        for (AgvReturnItemDO item : waveItems) {
            if (reservedTransits.add(item.getTransitSlotId())) {
                slotMapper.reserveIfEmpty(item.getTransitSlotId());
            }
        }
        String agvTaskId = plan.getTaskId() + "-UNLOAD-" + wave;
        List<Map<String, Object>> commands = new ArrayList<>();
        for (AgvReturnItemDO item : waveItems) {
            Map<String, Object> cmd = new LinkedHashMap<>();
            cmd.put("taskType", plan.getTaskType());
            cmd.put("plateType", resolvePlateType(plan, item));
            cmd.put("step3", JSON.parseObject(item.getStep3Json(), Map.class));
            commands.add(cmd);
        }
        sendAndEnqueue(plan, agvTaskId, "UNLOAD",
                Map.of("taskId", agvTaskId, "agvCmdList", commands, "disableSysFieldInjection", true));
    }

    private void afterUnloadComplete(AgvReturnPlanDO plan) {
        int wave = plan.getCurrentWave();
        List<AgvReturnItemDO> waveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), wave) && "ON_AGV".equals(item.getStatus())).toList();
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
        try {
            requestNextWarehouseReturn(plan, wave);
        } catch (Exception e) {
            // 不能让这一步的异常把下面的槽位释放重试、提前卸下一波都跟着跳过——这一步失败
            // 只影响"这一件物料现在能不能立刻开始入库"，不该连累其它跟它无关的收尾动作。
            // requestNextWarehouseReturn 内部已经把这次失败的副作用（warehouseRequestId）
            // 撤销干净，这里只是兜底记录一下，真正的重试交给事件驱动的下一次触发或
            // AgvReturnWarehouseWindowWatchdog兜底巡检。
            log.warn(
                    "[AgvReturn] 波次{}第一件入库请求失败，等下一次触发（同波次其它物料的回调，或兜底巡检）再重试 taskId={}",
                    wave, plan.getTaskId(), e);
        }
        // AGV槽位在这里释放（下料流程的主要释放点），触发一次编排排队重试。
        // afterUnloadComplete 本身跑在 advanceAfterTaskStateAsync 的 REQUIRES_NEW 事务里，
        // 同样需要等这个事务提交后才能重试，否则重试时会读到还没提交的旧槽位状态。
        scheduleOrchestrateQueueRetry(plan.getDeviceId());
        log.info("[AgvReturn] 波次{}卸料完成，等待仓储入库 taskId={}", wave, plan.getTaskId());

        // AGV这一波已经卸完、物理上已经空出来了，不用死等仓储把这一波全部取走(RETURNED)才让它
        // 干下一波——如果下一波要用的中转位这一刻恰好已经是空的（不是复用这一波还没被取走的
        // 位置），就立刻让AGV去卸下一波，跟仓储正在搬这一波的动作并行，等效于补料侧
        // AgvLoadPlanServiceImpl 的"抓当前件、仓储并行备下一件"。如果下一波要复用的中转位
        // 还被这一波占着（还没被仓储取走），下面会检测到并放弃，安全地退回原有的
        // "等这一波全部RETURNED再卸下一波"逻辑，不会把AGV派去卸到一个还占着东西的中转位。
        tryPrefetchNextUnloadWave(plan);
    }

    /**
     * 尝试提前把下一波卸料任务派给AGV，不等当前波全部被仓储取走(RETURNED)。
     *
     * <p>安全性：只有当下一波所需的全部中转位当前状态都是 EMPTY 才会真正派发；只要有一个
     * 还被占用（说明是复用当前波尚未被取走的中转位），就直接放弃，交回原有"等 markSlotReturned
     * 判断 waveComplete 后再 sendUnloadWave"的流程处理，不会有物理上把物料卸到占用位的风险。
     *
     * <p>提前派发失败（比如仓储恰好在这一刻拒绝、或AGV队列异常）时把 currentWave 状态退回去，
     * 交给原有基于 RETURNED 回调的推进逻辑重新触发，不影响主流程正确性，只是错失一次提速机会。
     */
    private void tryPrefetchNextUnloadWave(AgvReturnPlanDO plan) {
        if (plan.getCurrentWave() >= plan.getTotalWaves())
            return; // 没有下一波了
        int nextWave = plan.getCurrentWave() + 1;
        List<AgvReturnItemDO> nextWaveItems = itemMapper.selectByTaskId(plan.getTaskId()).stream()
                .filter(item -> Objects.equals(item.getWaveNo(), nextWave)).toList();
        if (nextWaveItems.isEmpty())
            return;
        boolean allTransitSlotsFree = nextWaveItems.stream().map(AgvReturnItemDO::getTransitSlotId).distinct()
                .allMatch(slotId -> {
                    SlotInfoDO slot = slotMapper.selectBySlotId(slotId);
                    return slot != null && "EMPTY".equals(slot.getStatus());
                });
        if (!allTransitSlotsFree) {
            log.info("[AgvReturn] 下一波{}要用的中转位还被占用，暂不提前卸料，等当前波全部入库 taskId={}", nextWave,
                    plan.getTaskId());
            return;
        }
        plan.setCurrentWave(nextWave);
        planMapper.updateById(plan);
        try {
            sendUnloadWave(plan);
            log.info("[AgvReturn] 下一波{}中转位已空闲，提前派AGV卸料，与仓储搬运当前波并行 taskId={}", nextWave,
                    plan.getTaskId());
        } catch (Exception e) {
            plan.setCurrentWave(nextWave - 1);
            planMapper.updateById(plan);
            log.warn("[AgvReturn] 提前卸第{}波失败，退回等待原有RETURNED触发流程 taskId={}", nextWave, plan.getTaskId(),
                    e);
        }
    }

    /**
     * 向仓储请求下一件物料入库。
     *
     * <p>仓储机械臂同一时刻只能处理一件搬运——跟补料侧 {@link AgvLoadPlanServiceImpl#refillWarehouseWindow}
     * 的 warehouseBusy 检查是同一个物理约束，必须等上一件的入库回调（{@link #markSlotReturned}）
     * 真正到了才能发下一件。此前这里只用 {@code selectNextPendingReturn} 检查"这一件物料自己有没有
     * 被请求过"，没检查"计划里是不是已经有别的物料请求还在途"：正常的回调驱动链路（afterUnloadComplete
     * 首次触发、markSlotReturned 收到回调后才 triggerNextStepAsync 发下一条）本身是串行的，不会露出
     * 这个洞；但每60秒跑一次的 {@link AgvReturnWarehouseWindowWatchdog} 兜底巡检是独立调用这个方法的，
     * 如果巡检节拍恰好落在"上一件请求已发出、仓储回调还没回来"的窗口内，会把这个正常的排队等待状态
     * 误判成"待处理"，在没等到上一件回调之前就把下一件的 prepareMaterials 提前发出去。
     *
     * @return true 表示有物料已发出请求（仓储在处理中）；false 表示当前波次无待请求项，或仓储正忙（上一件还没回调）
     */
    private boolean requestNextWarehouseReturn(AgvReturnPlanDO plan, int waveNo) {
        if (warehouseReturnBusy(plan.getTaskId()))
            return false;

        AgvReturnItemDO next = itemMapper.selectNextPendingReturn(plan.getTaskId(), waveNo);
        if (next == null)
            return false;

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

        try {
            DeviceCommandSpi.Result response = deviceCommandSpi.send("WAREHOUSE", "prepareMaterials", params,
                    plan.getTaskId(), "warehouse_return_" + next.getSequenceNo(), null);
            if (response.getResponseBody() != null && !response.getResponseBody().isBlank()) {
                Integer code = JSON.parseObject(response.getResponseBody()).getInteger("code");
                if (code != null && code != 0 && code != 1005) {
                    String message = JSON.parseObject(response.getResponseBody()).getString("message");
                    if (message == null)
                        message = JSON.parseObject(response.getResponseBody()).getString("msg");
                    throw exception(AGV_LOAD_PLAN_STATE_INVALID, "仓储拒绝入库 code=" + code + ", message=" + message);
                }
            }
        } catch (RuntimeException e) {
            // 跟补料侧 doRequestWarehouseItem 同一个坑：请求失败时必须把上面已经落库的
            // warehouseRequestId标记撤销，否则 selectNextPendingReturn（要求
            // warehouseRequestId IS NULL）会把这件物料永远当成"已经在请求中"，之后不管是
            // 同波次其它物料入库回调触发的自然重试，还是这次新增的兜底巡检Job，都不会再
            // 选中它，永久卡死。这里不需要像补料侧那样额外clearOccupancy——中转位是计划
            // 注册时一次性预留、AGV卸货时转OCCUPIED的，不受这次仓储请求失败影响，物料
            // 还老老实实待在中转位上，状态不用回滚。
            next.setWarehouseRequestId(null);
            itemMapper.updateById(next);
            throw e;
        }
        log.info("[AgvReturn] 请求仓储入库 taskId={} subRequestId={} from={} to={}", plan.getTaskId(), subRequestId,
                next.getTransitSlotId(), next.getWarehouseTargetLocation());
        return true;
    }

    /**
     * 本计划内是否还有物料的仓储入库请求已经发出、但还没等到 {@link #markSlotReturned} 回调
     * （status=IN_TRANSIT 且 warehouseRequestId 非空）。跟补料侧 refillWarehouseWindow 的
     * warehouseBusy 判断对称，用于在 {@link #requestNextWarehouseReturn} 里挡住兜底巡检的抢跑。
     */
    private boolean warehouseReturnBusy(String taskId) {
        return itemMapper.selectByTaskId(taskId).stream()
                .anyMatch(item -> "IN_TRANSIT".equals(item.getStatus()) && item.getWarehouseRequestId() != null);
    }

    /**
     * 仅设置状态（DB 持久化），流程回调由 triggerNextStepAsync 在事务提交后异步执行。
     */
    private void completeOrchestration(AgvReturnPlanDO plan) {
        plan.setStatus("WAIT_FLOW_COMMIT");
        planMapper.updateById(plan);
    }

    /**
     * 事务提交后推进流程：回调节点（仅在 WAIT_FLOW_COMMIT 时调用）。
     */
    private void doFlowCallback(AgvReturnPlanDO plan) {
        CallbackResult callback = stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null, true,
                Map.of("returnedCount", plan.getExpectedCount(), "warehouseReturn", true), null, null);
        if (!callback.isSuccess()) {
            throw exception(AGV_LOAD_PLAN_STATE_INVALID, "下料编排节点推进失败: " + callback.getErrorMsg());
        }
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void triggerNextStepAsync(String taskId) {
        try {
            // 等待 200ms，确保仓储设备已收到本次回调的 200 响应，再发下一条请求
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            // 加锁读取，跟补料那边同一处修复保持一致：这个方法和AGV taskState=2触发的
            // 另一条唤醒链路会并发读改同一个plan，不加锁的普通select理论上可能读到旧快照。
            AgvReturnPlanDO plan = planMapper.selectByTaskIdForUpdate(taskId);
            if (plan == null) {
                log.warn("[AgvReturn] 异步触发下一步：计划不存在 taskId={}", taskId);
                return;
            }
            if ("WAIT_FLOW_COMMIT".equals(plan.getStatus())) {
                // 全部入库完成，回调流程（事务已提交，安全调用）
                doFlowCallback(plan);
                log.info("[AgvReturn] 异步流程回调完成 taskId={}", taskId);
            } else {
                // 当前波次还有待请求的仓储入库 → 发下一条仓储请求；否则触发下一波 AGV 卸料
                int wave = plan.getCurrentWave();
                if (!requestNextWarehouseReturn(plan, wave)) {
                    sendUnloadWave(plan);
                    log.info("[AgvReturn] 异步触发第{}波卸料 taskId={}", wave, taskId);
                }
            }
        } catch (Exception e) {
            log.error("[AgvReturn] 异步触发下一步失败 taskId={}", taskId, e);
        }
    }

    /**
     * 释放某个计划占用的全部AGV/中转槽位。
     *
     * <p>只在计划彻底失败（AGV子任务物理失败）时调用——正常完成路径的槽位释放各自分散在
     * afterUnloadComplete/markSlotReturned/commitReturn 里，不受影响。clearOccupancy 本身
     * 没有前置状态校验，对RESERVED/OCCUPIED/已经EMPTY的槽位调用都是安全的。
     */
    private void releasePlanSlots(AgvReturnPlanDO plan) {
        for (AgvReturnItemDO item : itemMapper.selectByTaskId(plan.getTaskId())) {
            slotMapper.clearOccupancy(item.getAgvSlotId());
            slotMapper.clearOccupancy(item.getTransitSlotId());
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
     * 事务提交后异步触发一次编排排队重试，供释放AGV/中转槽位的地方调用。
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
        if (!"RETURN".equals(next.getPlanType())) {
            return; // LOAD计划的排队记录，交给AgvLoadPlanServiceImpl自己的触发处理
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
            // 必须走 self 代理调用，才能让 attemptStartReturnOrchestration 的 REQUIRES_NEW 真正生效
            self.attemptStartReturnOrchestration(next.getExecutionId(), next.getNodeId(), items, inputParams);
            orchestrateQueueMapper.markDispatched(next.getId());
            log.info("[AgvOrchestrateQueue] 排队重试成功，编排已启动 executionId={}", next.getExecutionId());
        } catch (ServiceException e) {
            if (!AGV_SLOT_UNAVAILABLE.getCode().equals(e.getCode())) {
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

    private Map<String, Object> handleWarehouseReturnFailure(AgvReturnPlanDO plan, WarehouseCallbackReqVO req) {
        AgvReturnItemDO failed = itemMapper.selectByTransitSlot(req.getTransferTaskId(), req.getFromLocation());
        if (failed == null)
            throw exception(SLOT_READY_CONFLICT, "找不到对应的下料入库请求");
        failed.setStatus("FAILED");
        itemMapper.updateById(failed);
        plan.setStatus("FAILED");
        planMapper.updateById(plan);
        if (plan.getOrchestrationNodeId() != null) {
            stepCallbackSpi.callback(plan.getTaskId(), plan.getOrchestrationNodeId(), null, false,
                    Map.of("slotId", req.getFromLocation()),
                    "WAREHOUSE_" + (req.getCode() != null ? req.getCode() : 1099),
                    req.getMessage() != null ? req.getMessage() : "仓储入库失败");
        }
        return result(plan, failed);
    }

    /**
     * 入队或立即调度 AGV startTask，并持久化计划状态。
     */
    private void sendAndEnqueue(AgvReturnPlanDO plan, String agvTaskId, String operation, Map<String, Object> params) {
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
        if (value == null)
            return defaultValue;
        int parsed = Integer.parseInt(String.valueOf(value));
        if (parsed <= 0)
            throw exception(AGV_LOAD_PLAN_INVALID, "数值必须大于0: " + value);
        return parsed;
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
        if (!(value instanceof Map<?, ?>))
            throw exception(AGV_LOAD_PLAN_INVALID, key + "不能为空");
        return (Map<String, Object>) value;
    }

    private String string(Object value, String defaultValue) {
        if (value == null || value.toString().isBlank())
            return defaultValue;
        return value.toString();
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean b)
            return b;
        return value != null && "true".equalsIgnoreCase(String.valueOf(value));
    }
}
