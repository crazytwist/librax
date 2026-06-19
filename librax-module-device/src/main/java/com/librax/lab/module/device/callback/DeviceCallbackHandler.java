package com.librax.lab.module.device.callback;

import com.librax.lab.module.device.codec.CodecExecutor;
import com.librax.lab.module.device.controller.app.vo.DeviceCallbackReqVO;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.mysql.devicecommand.DeviceCommandMapper;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import com.librax.lab.module.device.service.devicedirectexec.DeviceDirectExecService;
import com.librax.lab.module.device.service.devicedirectexec.DeviceDirectExecServiceImpl;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import com.librax.lab.module.flow.api.callback.TaskCallbackSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 设备回调处理器
 *
 * <p>统一处理设备完成回调：释放设备 → 解析响应 → 推进 DAG → 关闭 task 记录。
 * 同时被 {@code DeviceCallbackController}（webhook 模式）和
 * {@code DevicePollScheduler}（poll 模式）调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCallbackHandler {

    private final StepCallbackSpi         stepCallbackSpi;
    private final DeviceStateCache        stateCache;
    private final DeviceCommandMapper     commandMapper;
    private final CodecExecutor           codecExecutor;
    private final DeviceDirectExecService directExecService;

    /**
     * 可选注入：task 模块提供实现。
     * QUEUED+ASYNC 路径下，设备回调推进 DAG 后需关闭 lab_task 记录，
     * 防止 Watchdog 误判超时并重新入队导致指令重复发送。
     * DIRECT 路径无 lab_task 记录，此 SPI 会静默忽略。
     */
    @Autowired(required = false)
    private TaskCallbackSpi taskCallbackSpi;

    /**
     * 处理设备回调
     *
     * @param executionId 流程执行实例 ID
     * @param nodeId      步骤节点 ID
     * @param req         设备回调请求
     */
    public void handle(String executionId, String nodeId, DeviceCallbackReqVO req) {
        log.info("[DeviceCallback] 收到回调 executionId={} nodeId={} success={}",
                executionId, nodeId, req.isSuccess());

        // 直接执行回调：de- 前缀表示来自 DeviceDirectExec，不需要推进流程 DAG
        if (executionId.startsWith(DeviceDirectExecServiceImpl.EXEC_ID_PREFIX)) {
            handleDirectExec(executionId, nodeId, req);
            return;
        }

        // 1. 释放设备（通过反向索引 O(1) 找到 deviceId 并标记 IDLE）
        stateCache.markIdleByExecutionNode(executionId, nodeId);

        // 2. 解析输出数据
        Map<String, Object> outputs = resolveOutputs(req);

        // 3. 推进流程 DAG
        try {
            CallbackResult result = stepCallbackSpi.callback(
                    executionId,
                    nodeId,
                    req.getCallbackToken(),
                    req.isSuccess(),
                    outputs,
                    req.getErrorCode(),
                    req.getErrorMsg());

            if (!result.isSuccess()) {
                log.warn("[DeviceCallback] 推进失败 executionId={} nodeId={} error={} msg={}",
                        executionId, nodeId, result.getErrorCode(), result.getErrorMsg());
            }
        } catch (Exception e) {
            // 设备已完成，不因回调处理异常而重试（避免设备端重复回调）
            log.error("[DeviceCallback] 推进异常 executionId={} nodeId={}", executionId, nodeId, e);
        }

        // 4. 关闭对应的 lab_task 记录（QUEUED+ASYNC 路径）
        // DIRECT 路径无 lab_task 记录，TaskCallbackSpi 实现会静默忽略
        if (taskCallbackSpi != null && req.getCallbackToken() != null) {
            taskCallbackSpi.closeByToken(
                    req.getCallbackToken(),
                    req.isSuccess(),
                    outputs,
                    req.getErrorCode(),
                    req.getErrorMsg());
        }
    }

    /**
     * 解析输出数据
     * <p>
     * 如果请求中包含 rawResponse + deviceType + commandCode，
     * 则通过 CodecExecutor 解析原始报文；否则直接使用 data 字段。
     */
    private Map<String, Object> resolveOutputs(DeviceCallbackReqVO req) {
        // 情况1：设备发送了原始报文，需要解析
        if (req.getRawResponse() != null
                && req.getDeviceType() != null
                && req.getCommandCode() != null) {
            try {
                DeviceCommandDO command = commandMapper.selectByTypeAndCode(
                        req.getDeviceType(), req.getCommandCode());
                if (command != null && command.getCodecId() != null) {
                    return codecExecutor.parse(command.getCodecId(), req.getRawResponse());
                }
                // 无 codec 配置，透传原始响应
                return Map.of("raw", req.getRawResponse());
            } catch (Exception e) {
                log.warn("[DeviceCallback] 响应解析失败，使用原始数据: {}", e.getMessage());
                return req.getData() != null ? req.getData() : Map.of("raw", req.getRawResponse());
            }
        }

        // 情况2：设备已经返回结构化数据
        return req.getData() != null ? req.getData() : Map.of();
    }

    /**
     * 处理直接执行回调（executionId 以 de- 开头）
     *
     * <p>此路径是兜底路由：当设备使用通用端点 /{executionId}/{nodeId} 且 executionId 以 de- 开头时触发。
     * 专用端点 /{execId}/direct 已在 DeviceCallbackController 中独立处理（含 token 校验和设备释放），
     * 本方法仅处理通过通用路由进来的情况，同样执行释放 + 写结果。
     */
    private void handleDirectExec(String execId, String nodeId, DeviceCallbackReqVO req) {
        log.info("[DeviceCallback] 直接执行回调（通用路由）execId={} success={}", execId, req.isSuccess());

        // 1. 释放设备
        stateCache.markIdleByExecutionNode(execId, nodeId);

        // 2. 解析输出
        Map<String, Object> outputs = resolveOutputs(req);

        // 3. 更新 Redis 执行记录
        directExecService.onCallback(
                execId,
                req.isSuccess(),
                outputs,
                req.getErrorCode(),
                req.getErrorMsg(),
                req.getRawResponse());
    }
}
