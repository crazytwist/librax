package com.librax.lab.module.device.gateway;


import com.librax.lab.module.device.enums.DeviceStatusEnum;

import java.util.Map;

public interface DeviceGateway {

    /**
     * 发送指令（非阻塞）
     * 返回 taskId，设备完成后通过 CallbackDispatcher 回调
     *
     * @param deviceType  设备类型，如 PH_METER
     * @param commandCode 指令代码，如 MEASURE
     * @param params      运行时参数，替换模板中的 ${xxx} 占位符
     * @param executionId 流程执行ID（回调时带回，用于匹配步骤）
     * @param callbackToken 回调令牌（校验用）
     * @return taskId 设备侧任务ID
     */
    String sendCommand(String deviceType,
                       String commandCode,
                       Map<String, Object> params,
                       String executionId,
                       String nodeId,
                       String callbackToken);

    /**
     * 查询设备状态（心跳/健康检查）
     */
    DeviceStatusEnum getStatus(String deviceId);

    /**
     * 主动取消任务（步骤超时时调用）
     */
    void cancel(String deviceId, String taskId);
}