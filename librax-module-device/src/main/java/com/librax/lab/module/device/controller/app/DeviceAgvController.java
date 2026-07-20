package com.librax.lab.module.device.controller.app;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.tenant.core.aop.TenantIgnore;
import com.librax.lab.module.device.api.AgvWaitSignalHandler;
import com.librax.lab.module.device.api.AgvTaskStateHandler;
import com.librax.lab.module.device.api.AgvResumeHandler;
import com.librax.lab.module.device.callback.DeviceCallbackHandler;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import com.librax.lab.module.device.controller.app.vo.AgvTaskStateReqVO;
import com.librax.lab.module.device.controller.app.vo.AgvWaitSignalReqVO;
import com.librax.lab.module.device.controller.app.vo.DeviceCallbackReqVO;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

/**
 * AGV 标准接口适配层。
 *
 * <p>正式搬运流程把 executionId 直接作为 AGV taskId，下发和回调全程保持不变；
 * 流程节点由回调类型（waitSignal / taskState）确定，不再拆分带下划线的 taskId。
 */
@Slf4j
@RestController
@RequestMapping("device/agv")
@RequiredArgsConstructor
public class DeviceAgvController {

    private static final String DEFAULT_WAIT_NODE_ID = "s_agv_wait_signal";

    private final DeviceCallbackHandler deviceCallbackHandler;
    private final DeviceStateCache deviceStateCache;
    private final StepCallbackSpi stepCallbackSpi;
    private final List<AgvWaitSignalHandler> waitSignalHandlers;
    private final List<AgvTaskStateHandler> taskStateHandlers;
    private final List<AgvResumeHandler> resumeHandlers;

    /**
     * AGV 通知服务端：当前任务已运行到等待点，需要服务端确认后再继续。
     *
     * <p>该接口不直接下发 startAgain，而是先唤醒流程中的 WAIT 节点；后续 DAG 会自然执行
     * {@code agv_start_again} 节点，由设备网关调用 AGV 的 /device/agv/startAgain。
     */
    @PostMapping("/waitSignal")
    @PermitAll
    @TenantIgnore
    public CommonResult<Boolean> waitSignal(@Valid @RequestBody AgvWaitSignalReqVO req) {
        String executionId = resolveExecutionId(req.getExecutionId(), req.getTaskId());
        String waitNodeId = hasText(req.getNodeId()) ? req.getNodeId() : DEFAULT_WAIT_NODE_ID;

        // 多轮装载协调器优先消费信号。被消费后，中间波次不会推进固定的流程 WAIT 节点。
        for (AgvWaitSignalHandler handler : waitSignalHandlers) {
            if (handler.handle(req.getTaskId(), req.getAgvStation())) {
                log.info("[AGV] waitSignal由业务协调器处理 taskId={} agvStation={}",
                        req.getTaskId(), req.getAgvStation());
                return success(true);
            }
        }

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("taskId", req.getTaskId());
        outputs.put("agvStation", req.getAgvStation());
        outputs.put("signal", "WAIT_SIGNAL");
        outputs.put("receivedAt", LocalDateTime.now().toString());

        log.info("[AGV] waitSignal taskId={} executionId={} waitNodeId={}",
                req.getTaskId(), executionId, waitNodeId);

        CallbackResult result = stepCallbackSpi.callback(
                executionId,
                waitNodeId,
                req.getCallbackToken(),
                true,
                outputs,
                null,
                null);

        if (result.isSuccess()) {
            return success(true);
        }
        return CommonResult.error(400, result.getErrorMsg());
    }

    /**
     * AGV 任务状态回调。
     *
     * <p>runState=1 仅记录运行中，不推进流程；runState=2 推进成功；
     * runState=255 推进失败。推进设备节点时复用通用 DeviceCallbackHandler，
     * 确保设备状态、资源释放、DAG 推进逻辑一致。
     */
    @PostMapping("/taskState")
    @PermitAll
    @TenantIgnore
    public CommonResult<Boolean> taskState(@Valid @RequestBody AgvTaskStateReqVO req) {
        for (AgvTaskStateHandler handler : taskStateHandlers) {
            if (handler.handle(req.getTaskId(), req.getRunState(), req.getAgvId())) {
                log.info("[AGV] taskState由业务协调器处理 taskId={} runState={}",
                        req.getTaskId(), req.getRunState());
                return success(true);
            }
        }
        String executionId = resolveExecutionId(req.getExecutionId(), req.getTaskId());

        // runState=1 仅表示运行中，不推进流程
        if ("1".equals(req.getRunState())) {
            log.info("[AGV] taskState RUNNING taskId={} executionId={}", req.getTaskId(), executionId);
            return success(true);
        }

        // 优先使用 AGV 回传的 nodeId；若 AGV 不回传，从 Redis 查当前活跃节点
        String nodeId = hasText(req.getNodeId())
                ? req.getNodeId()
                : deviceStateCache.getActiveNodeByExecution(executionId);

        if (!hasText(nodeId)) {
            log.warn("[AGV] taskState 无法定位 nodeId，忽略回调 taskId={} executionId={}",
                    req.getTaskId(), executionId);
            return CommonResult.error(400, "无法定位当前等待节点，请检查流程状态");
        }

        boolean success = "2".equals(req.getRunState());
        DeviceCallbackReqVO callbackReq = new DeviceCallbackReqVO();
        callbackReq.setCallbackToken(req.getCallbackToken());
        callbackReq.setSuccess(success);
        callbackReq.setErrorCode(success ? null : "AGV_TASK_ERROR");
        callbackReq.setErrorMsg(success ? null : "AGV runState=" + req.getRunState());
        callbackReq.setData(Map.of(
                "taskId",     req.getTaskId(),
                "runState",   req.getRunState(),
                "agvId",      req.getAgvId() != null ? req.getAgvId() : "",
                "finishedAt", LocalDateTime.now().toString()
        ));

        log.info("[AGV] taskState terminal taskId={} executionId={} nodeId={} success={}",
                req.getTaskId(), executionId, nodeId, success);

        deviceCallbackHandler.handle(executionId, nodeId, callbackReq);
        return success(true);
    }

    /** 服务端完成开门等现场动作后，明确通知当前AGV子任务继续。 */
    @PostMapping("/startAgain")
    public CommonResult<Boolean> startAgain(@RequestBody Map<String, String> request) {
        String taskId = request.get("taskId");
        if (!hasText(taskId)) throw new IllegalArgumentException("taskId不能为空");
        for (AgvResumeHandler handler : resumeHandlers) {
            if (handler.resume(taskId)) return success(true);
        }
        return CommonResult.error(400, "任务不处于现场动作等待状态: " + taskId);
    }

    private String resolveExecutionId(String executionId, String taskId) {
        if (hasText(executionId)) {
            return executionId;
        }
        // 正式 AGV 流程约定 taskId 直接使用 executionId，不再拆分带下划线的业务任务号。
        if (!hasText(taskId)) {
            throw new IllegalArgumentException("AGV taskId不能为空");
        }
        return taskId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
