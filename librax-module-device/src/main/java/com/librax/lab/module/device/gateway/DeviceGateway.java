package com.librax.lab.module.device.gateway;


import com.librax.lab.module.device.driver.DeviceSendResult;
import com.librax.lab.module.device.enums.DeviceStatusEnum;

import java.util.Map;

public interface DeviceGateway {

    /**
     * 发送指令
     *
     * <p>返回 {@link DeviceSendResult}，其中 {@code syncCompleted=true} 表示指令在 HTTP 响应阶段
     * 即已完成（completionMode=SYNC），调用方可直接推进下一节点，无需等待设备回调。
     *
     * @param deviceType    设备类型，如 PH_METER
     * @param commandCode   指令代码，如 MEASURE
     * @param params        运行时参数，替换模板中的 ${xxx} 占位符
     * @param executionId   流程执行 ID（回调时带回，用于匹配步骤）
     * @param nodeId        节点 ID
     * @param callbackToken 回调令牌（校验用）
     * @return 发送结果
     */
    DeviceSendResult sendCommand(String deviceType,
                                 String commandCode,
                                 Map<String, Object> params,
                                 String executionId,
                                 String nodeId,
                                 String callbackToken);

    default DeviceSendResult sendCommand(String deviceType,
                                         String commandCode,
                                         Map<String, Object> params,
                                         String executionId,
                                         String nodeId,
                                         String callbackToken,
                                         boolean forceExec) {
        return sendCommand(deviceType, commandCode, params, executionId, nodeId, callbackToken);
    }

    /**
     * 查询设备状态（心跳/健康检查）
     */
    DeviceStatusEnum getStatus(String deviceId);

    /**
     * 发送指令到指定设备（跳过设备选择器，适用于直接执行场景）
     */
    DeviceSendResult sendCommandToDevice(String deviceId,
                                         String commandCode,
                                         Map<String, Object> params,
                                         String executionId,
                                         String nodeId,
                                         String callbackToken);

    /**
     * 强制发送指令到指定设备，跳过空闲状态校验（调试专用）
     */
    default DeviceSendResult sendCommandToDevice(String deviceId,
                                                  String commandCode,
                                                  Map<String, Object> params,
                                                  String executionId,
                                                  String nodeId,
                                                  String callbackToken,
                                                  boolean forceExec) {
        return sendCommandToDevice(deviceId, commandCode, params, executionId, nodeId, callbackToken);
    }

    /**
     * 主动取消任务（步骤超时时调用）
     */
    void cancel(String deviceId, String taskId);
}