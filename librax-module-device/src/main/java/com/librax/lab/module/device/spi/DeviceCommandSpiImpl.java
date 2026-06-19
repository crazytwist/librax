package com.librax.lab.module.device.spi;

import com.librax.lab.module.device.driver.DeviceSendResult;
import com.librax.lab.module.device.gateway.DeviceGateway;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * {@link DeviceCommandSpi} 的设备模块实现
 *
 * <p>供 task 模块的 {@code InstrumentTaskExecutor} 在 QUEUED 路径下调用，
 * 内部委托 {@link DeviceGateway} 完成设备选择、请求模板渲染、驱动发送全流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCommandSpiImpl implements DeviceCommandSpi {

    private final DeviceGateway deviceGateway;

    @Override
    public Result send(String deviceType,
                       String commandCode,
                       Map<String, Object> params,
                       String executionId,
                       String nodeId,
                       String callbackToken) {
        log.info("[DeviceCommandSpi] 发送设备指令 deviceType={} commandCode={} executionId={} nodeId={}",
                deviceType, commandCode, executionId, nodeId);

        DeviceSendResult sendResult = deviceGateway.sendCommand(
                deviceType, commandCode, params, executionId, nodeId, callbackToken);

        if (sendResult.isSyncCompleted()) {
            log.info("[DeviceCommandSpi] 同步完成 taskId={}", sendResult.getTaskId());
            return Result.sync(sendResult.getTaskId(), sendResult.getResponseBody());
        }

        log.info("[DeviceCommandSpi] 异步模式，等待设备回调 taskId={}", sendResult.getTaskId());
        return Result.async(sendResult.getTaskId());
    }
}
