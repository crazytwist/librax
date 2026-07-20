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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    private final AgvTaskQueueMapper queueMapper;
    private final AgvLoadPlanMapper loadPlanMapper;
    private final AgvReturnPlanMapper returnPlanMapper;
    private final DeviceCommandSpi deviceCommandSpi;

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
        next.setStatus("DISPATCHED");
        next.setDispatchTime(LocalDateTime.now());
        queueMapper.updateById(next);

        @SuppressWarnings("unchecked")
        Map<String, Object> params = JSON.parseObject(next.getPayload(), Map.class);
        deviceCommandSpi.send("AGV", "startTask", params,
                next.getPlanTaskId(),
                "agv_queued_" + next.getPlanType().toLowerCase() + "_" + next.getOperation().toLowerCase(),
                null);
        log.info("[AgvQueue] 调度下一任务 planType={} planTaskId={} agvTaskId={} op={}",
                next.getPlanType(), next.getPlanTaskId(), next.getAgvTaskId(), next.getOperation());
    }

    // ── 私有方法 ──────────────────────────────────────────────────────────────

    private String doEnqueue(String planType, String planTaskId, String deviceId,
                              String agvTaskId, String operation, Map<String, Object> params) {
        boolean idle = !queueMapper.hasDispatched(deviceId);
        LocalDateTime now = LocalDateTime.now();
        queueMapper.insert(AgvTaskQueueDO.builder()
                .deviceId(deviceId)
                .planTaskId(planTaskId)
                .agvTaskId(agvTaskId)
                .planType(planType)
                .operation(operation)
                .payload(JSON.toJSONString(params))
                .status(idle ? "DISPATCHED" : "PENDING")
                .createTime(now)
                .dispatchTime(idle ? now : null)
                .build());
        if (idle) {
            deviceCommandSpi.send("AGV", "startTask", params,
                    planTaskId, "agv_" + planType.toLowerCase() + "_" + operation.toLowerCase(), null);
            log.info("[AgvQueue] 立即调度 planType={} planTaskId={} agvTaskId={} op={}",
                    planType, planTaskId, agvTaskId, operation);
            return "WAIT_AGV_" + operation;
        } else {
            log.info("[AgvQueue] AGV 繁忙，入队等待 planType={} planTaskId={} agvTaskId={} op={}",
                    planType, planTaskId, agvTaskId, operation);
            return "QUEUED";
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
