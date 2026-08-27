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
import java.util.List;
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

    /** lab_device_command 里 device_type=AGV, command_code=GetAgvStatus 那条指令（id=910186，查真实状态）。 */
    private static final String QUERY_STATE_COMMAND_CODE = "GetAgvStatus";

    /** GetAgvStatus 返回的 state 枚举值：1空闲 2运行中 3充电中 4错误。 */
    private static final String AGV_STATE_IDLE = "1";
    private static final String AGV_STATE_RUNNING = "2";
    private static final String AGV_STATE_CHARGING = "3";
    private static final String AGV_STATE_ERROR = "4";

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
        AgvReturnPlanDO returnPlanOwner = activeReturnPlanOwner(deviceId);
        AgvTaskQueueDO next;
        if (returnPlanOwner != null) {
            // 下料计划从装载开始一直持有 AGV，直到仓储确认全部入库。若它正在等仓储腾空
            // 中转位而没有自身待发任务，不能改派分析站或其它计划。
            next = queueMapper.selectNextPendingByPlanTaskId(deviceId, returnPlanOwner.getTaskId());
            if (next == null) {
                log.info("[AgvQueue] 下料计划仍在回仓，暂不调度其它计划 deviceId={} returnTaskId={} status={}",
                        deviceId, returnPlanOwner.getTaskId(), returnPlanOwner.getStatus());
                return;
            }
        } else {
            next = queueMapper.selectNextPending(deviceId);
        }
        if (next == null) {
            log.info("[AgvQueue] 队列已空，AGV 空闲 deviceId={}", deviceId);
            return;
        }
        Object plan = lockPlanForDispatch(next);
        if (plan == null) {
            next.setStatus("CANCELLED");
            queueMapper.updateById(next);
            log.warn("[AgvQueue] 计划不可调度，跳过 planType={} planTaskId={} op={}",
                    next.getPlanType(), next.getPlanTaskId(), next.getOperation());
            dispatchNext(deviceId);
            return;
        }

        if (trySendStartTask(next, "agv_queued_" + next.getPlanType().toLowerCase() + "_" + next.getOperation().toLowerCase())) {
            markPlanDispatched(next, plan);
            log.info("[AgvQueue] 调度下一任务 planType={} planTaskId={} agvTaskId={} op={}",
                    next.getPlanType(), next.getPlanTaskId(), next.getAgvTaskId(), next.getOperation());
        } else {
            // 计划状态保持QUEUED不动（没有像以前那样提前改成WAIT_AGV_{operation}），
            // 只是AGV这次没接受指令，安排一次延迟重试；不递归调度下一条——它本来就该排后面。
            // 计划状态留在QUEUED，是为了让下一次重试的lockPlanForDispatch校验还能通过——
            // 之前这里在trySendStartTask之前就无条件把计划改成WAIT_AGV_{operation}，如果
            // 紧接着trySendStartTask失败，计划就永久卡在WAIT_AGV_{operation}但队列项还是
            // PENDING，下一次重试再进来时lockPlanForDispatch会发现"计划不是QUEUED"，把
            // 这个原本只是"AGV暂时忙"的任务当成"计划已失效"直接CANCELLED掉、整个任务
            // 悄悄丢失——这是2026-08-20被真实日志复现的一个bug：AGV忙的时间只要跨过
            // 两次2秒重试（约4秒），任务就会被误判丢弃，而不是继续按预期无限重试下去。
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
        AgvReturnPlanDO returnPlanOwner = activeReturnPlanOwner(deviceId);
        boolean heldByAnotherReturnPlan = returnPlanOwner != null
                && !returnPlanOwner.getTaskId().equals(planTaskId);
        boolean idle = !heldByAnotherReturnPlan && !queueMapper.hasDispatched(deviceId);
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
            log.info("[AgvQueue] AGV繁忙或被下料计划占用，入队等待 planType={} planTaskId={} agvTaskId={} op={} owner={}",
                    planType, planTaskId, agvTaskId, operation,
                    returnPlanOwner != null ? returnPlanOwner.getTaskId() : null);
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
     * <p>发送前先查一次 AGV 真实状态（{@link #queryAgvState}），不再只靠系统内部队列表/
     * 内存充电标记去猜测AGV是不是真的能接单——之前系统对"AGV忙不忙"完全没有主动核实的
     * 手段，只能等 startTask 被 AGV 自己用 code=1 拒绝后被动发现（见 {@link #isAgvAccepted}），
     * 中间有个"系统以为空闲、设备其实还在忙"的盲区。这里只是把已知会被拒绝的两种情况
     * （运行中/错误）提前拦掉，省一次必然失败的往返；查不到状态或状态是其它值时，
     * 仍然照发，最终由 AGV 自己的拒绝机制兜底，不会因为这次查询引入新的卡点。
     *
     * @return true 表示AGV接受了这条指令
     */
    private boolean trySendStartTask(AgvTaskQueueDO entry, String nodeId) {
        String deviceId = entry.getDeviceId();
        String agvState = queryAgvState(deviceId);

        if (AGV_STATE_RUNNING.equals(agvState) || AGV_STATE_ERROR.equals(agvState)) {
            // 真实状态已经确认AGV现在忙（运行中）或出错，这次必然会被拒绝，直接不发，
            // 跟AGV自己拒绝时一样走2秒后重试。error不会自己变回idle，会一直重试到人工
            // 处理为止，跟其它卡死场景的兜底方式保持一致，这里不额外做告警/中断。
            log.info("[AgvQueue] AGV真实状态={}（非空闲/充电），暂不下发，{}ms后重试 deviceId={}",
                    agvState, DISPATCH_RETRY_DELAY_MS, deviceId);
            return false;
        }

        if (AGV_STATE_CHARGING.equals(agvState) || chargingStateTracker.isCharging(deviceId)) {
            // 现在AGV充电纯粹是"反正闲着顺手充"（不存在电量不够被打断的场景），不用管
            // 这次充电是系统自己下发的还是AGV自行触发的，直接先发一次停止充电指令再继续
            // 派任务。不在这里清除充电标记——如果Cancel本身没生效或者没来得及生效，
            // 紧接着的startTask大概率还是会被拒绝，走下面已有的拒绝重试机制（2秒后重试）；
            // 只要标记还在，下一次重试会再发一次Cancel，直到某次startTask真正被接受，
            // 才在下面成功分支里清除标记——避免"以为发了就一定生效"的乐观假设。
            log.info("[AgvQueue] AGV正在充电（真实状态={}，系统充电标记={}），先发送停止充电指令 deviceId={}",
                    agvState, chargingStateTracker.isCharging(deviceId), deviceId);
            deviceCommandSpi.send("AGV", CANCEL_CHARGING_COMMAND_CODE,
                    Map.of("disableSysFieldInjection", true),
                    "SYSTEM-AGV-CANCEL-CHARGE-" + deviceId, "agv_cancel_charging", null);
        } else if (AGV_STATE_IDLE.equals(agvState)) {
            log.info("[AgvQueue] AGV真实状态=空闲，直接下发 deviceId={}", deviceId);
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

    /**
     * 查询 AGV 真实状态（GetAgvStatus，同步 HTTP GET /device/agv/getAgvState，响应体
     * {@code {"state":"1"}}）。查询本身失败（网络异常、超时、响应体解析不出来）时按
     * "未知"处理返回 null——查不到状态不代表AGV真的忙，不能让一次查询失败就把整个调度
     * 卡住，调用方对 null 的处理是照常发 startTask，由AGV自己的拒绝机制兜底。
     *
     * @return "1"空闲 / "2"运行中 / "3"充电中 / "4"错误；查询失败或解析不出来返回 null
     */
    private String queryAgvState(String deviceId) {
        try {
            DeviceCommandSpi.Result response = deviceCommandSpi.send("AGV", QUERY_STATE_COMMAND_CODE,
                    Map.of(), "SYSTEM-AGV-STATE-" + deviceId, "agv_query_state", null);
            if (response == null || response.getResponseBody() == null || response.getResponseBody().isBlank()) {
                return null;
            }
            return JSON.parseObject(response.getResponseBody()).getString("state");
        } catch (Exception e) {
            log.warn("[AgvQueue] 查询AGV真实状态失败，按未知处理，继续走原有流程 deviceId={}", deviceId, e);
            return null;
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
     * 校验队列项对应的计划是否仍处于可下发状态（QUEUED）并加行锁（防止同一计划被并发调度）。
     * 只读校验+加锁，不做任何状态修改——是否真的进入 WAIT_AGV_{operation}，要等
     * {@link #trySendStartTask} 确认AGV真的接受了指令之后，由 {@link #markPlanDispatched}
     * 单独完成。以前这两件事（校验+改状态）是在同一个方法里无条件一起做的：不管
     * trySendStartTask 后面成不成功，计划状态都会先被改成 WAIT_AGV_{operation}；一旦
     * trySendStartTask 失败，计划就卡在这个状态、队列项还是PENDING，下一次重试进来时
     * 这里的校验会发现"计划不是QUEUED"，把一个只是"AGV暂时忙"的任务误判成"计划已失效"
     * 直接取消掉——这个方法拆开就是为了堵上这个洞。
     *
     * @return 计划仍是 QUEUED 则返回锁定后的计划对象（LOAD 对应 {@link AgvLoadPlanDO}，
     *         RETURN 对应 {@link AgvReturnPlanDO}）；计划不存在或已不是 QUEUED 返回 null
     */
    private Object lockPlanForDispatch(AgvTaskQueueDO next) {
        if ("LOAD".equals(next.getPlanType())) {
            AgvLoadPlanDO plan = loadPlanMapper.selectByTaskIdForUpdate(next.getPlanTaskId());
            return (plan != null && "QUEUED".equals(plan.getStatus())) ? plan : null;
        } else {
            AgvReturnPlanDO plan = returnPlanMapper.selectByTaskIdForUpdate(next.getPlanTaskId());
            return (plan != null && "QUEUED".equals(plan.getStatus())) ? plan : null;
        }
    }

    /**
     * 返回当前最早创建且未终态的下料计划。它是该 AGV 的逻辑持有者，直到计划提交完成或失败。
     */
    private AgvReturnPlanDO activeReturnPlanOwner(String deviceId) {
        List<AgvReturnPlanDO> activePlans = returnPlanMapper.selectActiveByDeviceId(deviceId);
        return activePlans.isEmpty() ? null : activePlans.get(0);
    }

    /** trySendStartTask 确认AGV真的接受指令后，才把 {@link #lockPlanForDispatch} 锁定的计划改成 WAIT_AGV_{operation}。 */
    private void markPlanDispatched(AgvTaskQueueDO next, Object plan) {
        String newStatus = "WAIT_AGV_" + next.getOperation();
        if (plan instanceof AgvLoadPlanDO loadPlan) {
            loadPlan.setStatus(newStatus);
            loadPlanMapper.updateById(loadPlan);
        } else if (plan instanceof AgvReturnPlanDO returnPlan) {
            returnPlan.setStatus(newStatus);
            returnPlanMapper.updateById(returnPlan);
        }
    }
}
