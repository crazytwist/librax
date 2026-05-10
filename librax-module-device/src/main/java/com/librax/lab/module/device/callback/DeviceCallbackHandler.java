package com.librax.lab.module.device.callback;

import com.librax.lab.module.device.codec.CodecExecutor;
import com.librax.lab.module.device.controller.vo.DeviceCallbackReqVO;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.mysql.devicecommand.DeviceCommandMapper;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 设备回调处理器
 *
 * <p>统一处理设备完成回调：释放设备 → 解析响应 → 推进 DAG。
 * 同时被 {@code DeviceCallbackController}（webhook 模式）和
 * {@code DevicePollScheduler}（poll 模式）调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCallbackHandler {

    private final StepCallbackSpi stepCallbackSpi;
    private final DeviceStateCache stateCache;
    private final DeviceCommandMapper commandMapper;
    private final CodecExecutor codecExecutor;

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
}
