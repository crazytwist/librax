package com.librax.lab.module.device.gateway;


import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.dal.mysql.devicecommand.DeviceCommandMapper;
import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;
import com.librax.lab.module.device.driver.DeviceDriver;
import com.librax.lab.module.device.driver.DeviceDriverFactory;
import com.librax.lab.module.device.enums.DeviceStatus;
import com.librax.lab.module.device.exception.DeviceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceGatewayImpl implements DeviceGateway {

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceCommandMapper deviceCommandMapper;
    private final DeviceSelector deviceSelector;
    private final DeviceDriverFactory driverFactory;
    private final DeviceStateCache stateCache;

    @Override
    public String sendCommand(String deviceType,
                              String commandCode,
                              Map<String, Object> params,
                              String executionId,
                              String nodeId,
                              String callbackToken) {
        // 1. 加载指令配置
        DeviceCommandDO command = deviceCommandMapper
                .selectByTypeAndCode(deviceType, commandCode);
        if (command == null) {
            throw new DeviceException("指令配置不存在: " + deviceType + "." + commandCode);
        }

        // 2. 选择空闲设备
        DeviceInfoDO device = deviceSelector.select(deviceType);
        if (device == null) {
            throw new DeviceException("无可用设备: " + deviceType);
        }

        // 3. 渲染请求模板（替换 ${xxx} 占位符）
        String requestBody = renderTemplate(command.getRequestTemplate(), params);

        // 4. 获取对应驱动并发送
        DeviceDriver driver = driverFactory.getDriver(device.getProtocol());
        String taskId = driver.send(device, command, requestBody, executionId, callbackToken);

        // 5. 更新设备状态为 BUSY（同时写反向索引 executionId+nodeId → deviceId）
        stateCache.markBusy(device.getDeviceId(), taskId, executionId, nodeId);

        log.info("[DeviceGateway] 指令已发送 deviceId={} commandCode={} taskId={} executionId={}",
                device.getDeviceId(), commandCode, taskId, executionId);

        return taskId;
    }

    @Override
    public DeviceStatus getStatus(String deviceId) {
        return stateCache.getStatus(deviceId);
    }

    @Override
    public void cancel(String deviceId, String taskId) {
        DeviceInfoDO device = deviceInfoMapper.selectByDeviceId(deviceId);
        if (device == null) return;
        DeviceDriver driver = driverFactory.getDriver(device.getProtocol());
        driver.cancel(device, taskId);
        stateCache.markIdle(deviceId);
        log.info("[DeviceGateway] 任务已取消 deviceId={} taskId={}", deviceId, taskId);
    }

    /**
     * 渲染模板：把 ${key} 替换为 params 中对应的值
     */
    private String renderTemplate(String template, Map<String, Object> params) {
        if (template == null || params == null) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace(
                    "${" + entry.getKey() + "}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}