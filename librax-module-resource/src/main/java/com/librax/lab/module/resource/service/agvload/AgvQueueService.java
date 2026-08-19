package com.librax.lab.module.resource.service.agvload;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadPlanDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnPlanDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvTaskQueueDO;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvReturnPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvTaskQueueMapper;
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
import java.util.Map;

/**
 * AGV 任务队列服务。
 *
 * <p>同一台 AGV 的 startTask 必须串行下发。补料和下料计划共享同一队列（按 device_id）。
 *
 * <p>入队逻辑：检查 AGV 是否有 DISPATCHED 任务；
 * 若空闲则立即下发（status=DISPATCHED），否则排队（status=PENDING）。
 *
 * <p>出队逻辑：AGV 任务完成后调用 {@link #dispatchNext}，从 PENDING 队首取出并下发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgvQueueService {

    /** 自注入，用于在事务提交后通过 Spring 代理调用 @Async 方法 */
    @Lazy
    @Autowired
    private AgvQueueService self;

    /** AGV拒绝接收指令（比如还在处理上一次任务的收尾上报）后，延迟多久重试一次。 */
    private static final long DISPATCH_RETRY_DELAY_MS = 2000L;

    /** lab_device_command 里 device_type=AGV, command_code=Cancel 那条指令（id=910183，停止充电）。 */
    private static final String CANCEL_CHARGING_COMMAND_CODE = "Cancel";

    private final AgvTaskQueueMapper queueMapper;
    private final AgvLoadPlanMapper loadPlanMapper;
    private final AgvReturnPlanMapper returnPlanMapper;
    private final DeviceCommandSpi deviceCommandSpi;
    private final AgvChargingStateTracker chargingStateTracker;

    /**
     * 将 AGV startTask 请求入队或立即下发（用于补料计划）。
     *
     * @return 计划应更新到的新状态：{@code "WAIT_AGV_" + operation} 或 {@code "QUEUED"}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String enqueueOrDispatch(AgvLoadPlanDO plan, String agvTaskId,
                                    String operation, Map<String, Object> params) {
        return doEnqueue("LOAD", plan.getTaskId(), plan.getDeviceId(), agvTaskId, operation, params);
    }

    /**
     * 将 AGV startTask 请求入队或立即下发（用于下料计划）。
     *
     * @return 计划应更新到的新状态：{@code "WAIT_AGV_" + operation} 或 {@code "QUEUED"}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String enqueueOrDispatch(AgvReturnPlanDO plan, String agvTaskId,
                                    String operation, Map<String, Object> params) {
        return doEnqueue("RETURN", plan.getTaskId(), plan.getDeviceId(), agvTaskId, operation, params);
    }

    /**
     * 从队列调度下一个待执行任务（FIFO，跨补料/下料计划）。
     *
     * <p>通常在 AGV 任务完成（runState=2）后调用，或失败后让 AGV 继续处理其它排队任务。
     * 若对应计划已不在 QUEUED 状态，则跳过并递归调度下一条。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void dispatchNext(String deviceId) {
        AgvTaskQueueDO next = queueMapper.selectNextPending(deviceId);
        if (next == null) {
            log.info("[AgvQueue] 队列已空，AGV 空闲 deviceId={}", deviceId);
            return;
        }
        boolean valid = updatePlanStatusForDispatch(next);
        if (!valid) {
            next.setStatus("CANCELLED");
            queueMapper.updateById(next);
            log.warn("[AgvQueue] 计划不可调度，跳过 planType={} planTaskId={} op={}",
                    next.getPlanType(), next.getPlanTaskId(), next.getOperation());
            dispatchNext(deviceId);
            return;
        }

        if (trySendStartTask(next, "agv_queued_" + next.getPlanType().toLowerCase() + "_" + next.getOperation().toLowerCase())) {
            log.info("[AgvQueue] 调度下一任务 planType={} planTaskId={} agvTaskId={} op={}",
                    next.getPlanType(), next.getPlanTaskId(), next.getAgvTaskId(), next.getOperation());
        } else {
            // plan状态已经改成WAIT_AGV_{operation}了（语义没错，这件事确实排到它了），
            // 只是AGV这次没接受指令，安排一次延迟重试；不递归调度下一条——它本来就该排后面。
            scheduleDispatchRetry(deviceId);
        }
    }

    /**
     * 巡检兜底入口：仅当该设备"看起来空闲但还有排队项"时才触发一次调度。
     * 供 {@code AgvTaskQueueWatchdog} 定期调用，覆盖延迟重试耗尽或应用重启丢失重试的场景。
     * 自带事务（调用方通常不在事务中），正常情况下应该总是空转。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchNextIfIdle(String deviceId) {
        if (!queueMapper.hasDispatched(deviceId) && queueMapper.hasPending(deviceId)) {
            log.warn("[AgvQueue] 巡检发现设备空闲但仍有排队项，补发调度 deviceId={}", deviceId);
            dispatchNext(deviceId);
        }
    }

    // ── 私有方法 ──────────────────────────────────────────────────────────────

    private String doEnqueue(String planType, String planTaskId, String deviceId,
                              String agvTaskId, String operation, Map<String, Object> params) {
        boolean idle = !queueMapper.hasDispatched(deviceId);
        LocalDateTime now = LocalDateTime.now();
        AgvTaskQueueDO entry = AgvTaskQueueDO.builder()
                .deviceId(deviceId)
                .planTaskId(planTaskId)
                .agvTaskId(agvTaskId)
                .planType(planType)
                .operation(operation)
                .payload(JSON.toJSONString(params))
                .status("PENDING")
                .createTime(now)
                .build();
        queueMapper.insert(entry);
        if (!idle) {
            log.info("[AgvQueue] AGV 繁忙，入队等待 planType={} planTaskId={} agvTaskId={} op={}",
                    planType, planTaskId, agvTaskId, operation);
            return "QUEUED";
        }
        if (trySendStartTask(entry, "agv_" + planType.toLowerCase() + "_" + operation.toLowerCase())) {
            log.info("[AgvQueue] 立即调度 planType={} planTaskId={} agvTaskId={} op={}",
                    planType, planTaskId, agvTaskId, operation);
            return "WAIT_AGV_" + operation;
        }
        // AGV此刻拒绝接收（典型响应 code=1 "AGV运行中,无法响应"——AGV自己上一次taskState上报
        // 还没走完整个来回，认为自己还"忙"），不算真正下发成功：队列项留在PENDING，
        // 安排一次延迟重试，不在这里同步等（这个方法常在持有plan行锁的事务里被调用）。
        scheduleDispatchRetry(deviceId);
        return "QUEUED";
    }

    /**
     * 实际下发 startTask 并检查AGV是否真的接受了这条指令（不是仅仅HTTP 200）。
     * 接受则把队列项标记DISPATCHED；拒绝则保持队列项原状态（PENDING），由调用方安排重试。
     *
     * @return true 表示AGV接受了这条指令
     */
    private boolean trySendStartTask(AgvTaskQueueDO entry, String nodeId) {
        String deviceId = entry.getDeviceId();
        if (chargingStateTracker.isCharging(deviceId)) {
            // AgvIdleChargingWatchdog 判定AGV空闲后主动下发过充电指令，现在有真正的任务要
            // 用这台AGV了，充电期间AGV对新指令的响应能力可能受影响，直接发startTask大概率
            // 会被拒绝（跟"AGV运行中，无法响应"是同一类"暂时接不了新指令"的表现）。这里先发
            // 一次停止充电。不在这里清除充电标记——如果Cancel本身没生效或者没来得及生效，
            // 紧接着的startTask大概率还是会被拒绝，走下面已有的拒绝重试机制（2秒后重试）；
            // 只要标记还在，下一次重试会再发一次Cancel，直到某次startTask真正被接受，
            // 才在下面成功分支里清除标记——避免"以为发了就一定生效"的乐观假设。
            log.info("[AgvQueue] AGV处于系统下发的充电状态，先发送停止充电指令 deviceId={}", deviceId);
            deviceCommandSpi.send("AGV", CANCEL_CHARGING_COMMAND_CODE,
                    Map.of("disableSysFieldInjection", true),
                    "SYSTEM-AGV-CANCEL-CHARGE-" + deviceId, "agv_cancel_charging", null);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> params = JSON.parseObject(entry.getPayload(), Map.class);
        DeviceCommandSpi.Result response = deviceCommandSpi.send("AGV", "startTask", params,
                entry.getPlanTaskId(), nodeId, null);
        if (!isAgvAccepted(response)) {
            log.warn("[AgvQueue] AGV拒绝接收指令，{}ms后重试 deviceId={} agvTaskId={} responseBody={}",
                    DISPATCH_RETRY_DELAY_MS, deviceId, entry.getAgvTaskId(), response.getResponseBody());
            return false;
        }
        chargingStateTracker.markNotCharging(deviceId);
        entry.setStatus("DISPATCHED");
        entry.setDispatchTime(LocalDateTime.now());
        queueMapper.updateById(entry);
        return true;
    }

    /**
     * 判断AGV是否真的接受了这条startTask指令。已知会被拒绝的典型响应：
     * {@code {"code":1,"msg":"AGV运行中,无法响应"}}——AGV自己上一次任务的taskState上报
     * 还没收到我们的ACK、内部还认为自己"忙"，此时发新指令会被原样拒绝，HTTP层面仍是200。
     * 没有响应体（或非JSON）的情况下按接受处理，跟仓储那边保持一致的保守假设。
     */
    private boolean isAgvAccepted(DeviceCommandSpi.Result response) {
        if (response == null || response.getResponseBody() == null || response.getResponseBody().isBlank()) {
            return true;
        }
        try {
            Integer code = JSON.parseObject(response.getResponseBody()).getInteger("code");
            return code == null || code == 0;
        } catch (Exception e) {
            return true; // 响应体解析不出code（非预期格式），不要因为这个误判失败
        }
    }

    /** 事务提交后异步延迟重试一次调度（AGV刚才拒绝接收指令时用）。 */
    private void scheduleDispatchRetry(String deviceId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.retryDispatchAfterRejectionAsync(deviceId);
            }
        });
    }

    /**
     * 延迟重试调度入口，事务提交后由 {@link #scheduleDispatchRetry} 异步调用。
     * 只供内部通过 self 代理自调用，不直接对外暴露。
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void retryDispatchAfterRejectionAsync(String deviceId) {
        try {
            Thread.sleep(DISPATCH_RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            dispatchNext(deviceId);
        } catch (Exception e) {
            log.error("[AgvQueue] 延迟重试调度失败 deviceId={}", deviceId, e);
        }
    }

    /**
     * 将队列项对应计划的状态更新为 WAIT_AGV_{operation}。
     *
     * @return false 表示计划已无效（不在 QUEUED 状态），应跳过该队列项
     */
    private boolean updatePlanStatusForDispatch(AgvTaskQueueDO next) {
        String newStatus = "WAIT_AGV_" + next.getOperation();
        if ("LOAD".equals(next.getPlanType())) {
            AgvLoadPlanDO plan = loadPlanMapper.selectByTaskIdForUpdate(next.getPlanTaskId());
            if (plan == null || !"QUEUED".equals(plan.getStatus())) return false;
            plan.setStatus(newStatus);
            loadPlanMapper.updateById(plan);
        } else {
            AgvReturnPlanDO plan = returnPlanMapper.selectByTaskIdForUpdate(next.getPlanTaskId());
            if (plan == null || !"QUEUED".equals(plan.getStatus())) return false;
            plan.setStatus(newStatus);
            returnPlanMapper.updateById(plan);
        }
        return true;
    }
}
