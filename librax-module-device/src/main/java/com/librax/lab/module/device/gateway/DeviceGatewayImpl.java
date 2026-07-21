package com.librax.lab.module.device.gateway;


import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.dal.mysql.devicecommand.DeviceCommandMapper;
import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;
import com.librax.lab.module.device.driver.DeviceDriver;
import com.librax.lab.module.device.driver.DeviceDriverFactory;
import com.librax.lab.module.device.driver.DeviceSendResult;
import com.librax.lab.module.device.enums.DeviceStatusEnum;
import com.librax.lab.module.device.exception.DeviceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.librax.lab.framework.common.util.expression.ExpressionUtil;

import java.util.HashMap;
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
    public DeviceSendResult sendCommand(String deviceType,
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

        // 3. 渲染请求模板（替换 ${xxx} 占位符），并自动注入系统回调字段
        Map<String, Object> enrichedParams = withTaskId(params, executionId);
        String requestBody = injectSysFields(
                renderTemplate(command.getRequestTemplate(), enrichedParams),
                executionId, nodeId, callbackToken, enrichedParams);

        // 4. 获取对应驱动并发送
        DeviceDriver driver = driverFactory.getDriver(device.getProtocol());
        DeviceSendResult result = driver.send(device, command, requestBody, executionId, callbackToken);

        // 5. 更新设备状态为 BUSY（同时写反向索引 executionId+nodeId → deviceId）
        stateCache.markBusy(device.getDeviceId(), result.getTaskId(), executionId, nodeId);

        // 6. SYNC 模式：HTTP 响应即完成，标记同步完成后立即释放设备
        if ("SYNC".equalsIgnoreCase(command.getCompletionMode())) {
            stateCache.markIdle(device.getDeviceId());
            result.setSyncCompleted(true);
        }

        log.info("[DeviceGateway] 指令已发送 deviceId={} commandCode={} taskId={} syncCompleted={} executionId={}",
                device.getDeviceId(), commandCode, result.getTaskId(), result.isSyncCompleted(), executionId);

        return result;
    }

    @Override
    public DeviceStatusEnum getStatus(String deviceId) {
        return stateCache.getStatus(deviceId);
    }

    @Override
    public DeviceSendResult sendCommandToDevice(String deviceId,
                                                 String commandCode,
                                                 Map<String, Object> params,
                                                 String executionId,
                                                 String nodeId,
                                                 String callbackToken) {
        // 1. 加载设备信息
        DeviceInfoDO device = deviceInfoMapper.selectByDeviceId(deviceId);
        if (device == null) {
            throw new DeviceException("设备不存在: " + deviceId);
        }
        if (!Boolean.TRUE.equals(device.getEnabled())) {
            throw new DeviceException("设备已禁用: " + deviceId);
        }

        // 2. 加载指令配置（按设备类型+指令码查找）
        DeviceCommandDO command = deviceCommandMapper
                .selectByTypeAndCode(device.getDeviceType(), commandCode);
        if (command == null) {
            throw new DeviceException("指令配置不存在: " + device.getDeviceType() + "." + commandCode);
        }

        // 3. 渲染请求模板，并自动注入系统回调字段
        Map<String, Object> enrichedParams = withTaskId(params, executionId);
        String requestBody = injectSysFields(
                renderTemplate(command.getRequestTemplate(), enrichedParams),
                executionId, nodeId, callbackToken, enrichedParams);

        // 4. 获取驱动并发送
        DeviceDriver driver = driverFactory.getDriver(device.getProtocol());
        DeviceSendResult result = driver.send(device, command, requestBody, executionId, callbackToken);

        // 5. 更新设备状态为 BUSY（含反向索引 executionId+nodeId → deviceId）
        stateCache.markBusy(device.getDeviceId(), result.getTaskId(), executionId, nodeId);

        // 6. SYNC 模式：立即释放设备并标记同步完成
        if ("SYNC".equalsIgnoreCase(command.getCompletionMode())) {
            stateCache.markIdle(device.getDeviceId());
            result.setSyncCompleted(true);
        }

        log.info("[DeviceGateway] 指令已直发 deviceId={} commandCode={} taskId={} syncCompleted={} executionId={}",
                deviceId, commandCode, result.getTaskId(), result.isSyncCompleted(), executionId);

        return result;
    }

    @Override
    public DeviceSendResult sendCommand(String deviceType,
                                        String commandCode,
                                        Map<String, Object> params,
                                        String executionId,
                                        String nodeId,
                                        String callbackToken,
                                        boolean forceExec) {
        DeviceCommandDO command = deviceCommandMapper.selectByTypeAndCode(deviceType, commandCode);
        if (command == null) {
            throw new DeviceException("指令配置不存在: " + deviceType + "." + commandCode);
        }
        DeviceInfoDO device = deviceSelector.select(deviceType, null, forceExec);
        if (device == null) {
            throw new DeviceException("无可用设备: " + deviceType);
        }
        Map<String, Object> enrichedParams = withTaskId(params, executionId);
        String requestBody = injectSysFields(
                renderTemplate(command.getRequestTemplate(), enrichedParams),
                executionId, nodeId, callbackToken, enrichedParams);
        DeviceDriver driver = driverFactory.getDriver(device.getProtocol());
        DeviceSendResult result = driver.send(device, command, requestBody, executionId, callbackToken);
        stateCache.markBusy(device.getDeviceId(), result.getTaskId(), executionId, nodeId);
        if ("SYNC".equalsIgnoreCase(command.getCompletionMode())) {
            stateCache.markIdle(device.getDeviceId());
            result.setSyncCompleted(true);
        }
        log.info("[DeviceGateway] 指令已发送(force={}) deviceId={} commandCode={} taskId={} syncCompleted={}",
                forceExec, device.getDeviceId(), commandCode, result.getTaskId(), result.isSyncCompleted());
        return result;
    }

    @Override
    public DeviceSendResult sendCommandToDevice(String deviceId,
                                                 String commandCode,
                                                 Map<String, Object> params,
                                                 String executionId,
                                                 String nodeId,
                                                 String callbackToken,
                                                 boolean forceExec) {
        if (forceExec) {
            // 强制释放当前占用，确保下面 markBusy 能正常写入
            DeviceStatusEnum current = stateCache.getStatus(deviceId);
            if (current == DeviceStatusEnum.BUSY) {
                log.warn("[DeviceGateway] forceExec=true，强制释放 BUSY 设备 deviceId={}", deviceId);
                stateCache.markIdle(deviceId);
            }
        }
        return sendCommandToDevice(deviceId, commandCode, params, executionId, nodeId, callbackToken);
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

    private String renderTemplate(String template, Map<String, Object> params) {
        return ExpressionUtil.render(template, params);
    }

    /**
     * 将 executionId 作为 taskId 注入 params，模板中 ${taskId} 即可直接引用。
     * 若调用方已显式传入 taskId 则不覆盖。
     * 同时对 requestId 做兜底：若未传或传的是未解析的占位符（如 ${requestId}），自动生成 UUID。
     */
    private Map<String, Object> withTaskId(Map<String, Object> params, String executionId) {
        Map<String, Object> enriched = new HashMap<>(params != null ? params : Map.of());
        if (executionId != null) {
            enriched.putIfAbsent("taskId", executionId);
        }
        // requestId 兜底：未传 或 仍是占位符形式（${...}）时自动生成
        Object requestId = enriched.get("requestId");
        if (requestId == null || requestId.toString().isBlank()
                || requestId.toString().startsWith("${")) {
            enriched.put("requestId", java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        return enriched;
    }

    /**
     * 自动将系统回调字段注入到 JSON 请求体中。
     *
     * <p>无论 requestTemplate 是否配置了 ${executionId} 等占位符，
     * 发给设备的 body 都会携带这四个字段，设备可用于回调鉴权和流程推进。
     * 若 body 不是合法 JSON（如纯文本/二进制协议），原样返回不做修改。
     */
    private String injectSysFields(String body, String executionId, String nodeId,
                                   String callbackToken, Map<String, Object> params) {
        if (body == null || body.isBlank()) return body;
        if (disableSysFieldInjection(params)) {
            return body;
        }
        try {
            JSONObject json = JSON.parseObject(body);
            if (executionId   != null) json.put("executionId",   executionId);
            if (nodeId        != null) json.put("nodeId",        nodeId);
            if (callbackToken != null) json.put("callbackToken", callbackToken);
            // callbackUrl 来自流程执行时的 params，直接执行场景下为空
            Object callbackUrl = params != null ? params.get("callbackUrl") : null;
            if (callbackUrl   != null) json.put("callbackUrl",   callbackUrl.toString());
            return json.toJSONString();
        } catch (Exception e) {
            log.debug("[DeviceGateway] body 非 JSON 格式，跳过系统字段注入");
            return body;
        }
    }

    private boolean disableSysFieldInjection(Map<String, Object> params) {
        if (params == null) return false;
        Object disabled = params.get("disableSysFieldInjection");
        if (disabled instanceof Boolean b) return b;
        return disabled instanceof String s && "true".equalsIgnoreCase(s);
    }
}
