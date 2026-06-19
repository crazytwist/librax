package com.librax.lab.module.device.service.devicedirectexec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandExecuteReqVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandExecuteRespVO;
import com.librax.lab.module.device.driver.DeviceSendResult;
import com.librax.lab.module.device.gateway.DeviceGateway;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 设备指令直接执行 Service 实现
 *
 * <h3>Redis 存储结构</h3>
 * <pre>
 * Key  : device:direct:exec:{execId}   (Hash，TTL 2小时)
 * Field: execId / deviceType / commandCode / callbackToken /
 *        status / startTime / timeoutMs / deviceId / taskId /
 *        output / rawResponse / errorCode / errorMsg / executeMs / params
 * </pre>
 *
 * <h3>执行ID约定</h3>
 * execId 格式为 {@code de-{uuid}}，
 * {@link com.librax.lab.module.device.callback.DeviceCallbackHandler} 通过 {@code de-} 前缀
 * 将回调路由到本服务，而非推进流程 DAG。
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class DeviceDirectExecServiceImpl implements DeviceDirectExecService {

    /** execId 前缀，与流水线 executionId 区分，CallbackHandler 据此路由 */
    public static final String EXEC_ID_PREFIX = "de-";
    /** 独立执行时固定使用的 nodeId */
    public static final String NODE_ID = "direct";

    private static final String REDIS_PREFIX       = "device:direct:exec:";
    private static final long   EXPIRE_HOURS        = 2;
    private static final long   DEFAULT_TIMEOUT_MS  = 60_000L;

    private final DeviceGateway       deviceGateway;
    private final DeviceStateCache    stateCache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    // ----------------------------------------------------------------
    //  触发执行
    // ----------------------------------------------------------------

    @Override
    public DeviceCommandExecuteRespVO execute(DeviceCommandExecuteReqVO reqVO) {
        String execId        = EXEC_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
        String callbackToken = UUID.randomUUID().toString().replace("-", "");
        long   timeoutMs     = reqVO.getTimeoutMs() != null ? reqVO.getTimeoutMs() : DEFAULT_TIMEOUT_MS;
        long   startTime     = System.currentTimeMillis();

        // 1. 初始化 Redis 执行记录
        String redisKey = redisKey(execId);
        Map<String, String> state = new HashMap<>();
        state.put("execId",        execId);
        state.put("deviceType",    reqVO.getDeviceType());
        state.put("commandCode",   reqVO.getCommandCode());
        state.put("callbackToken", callbackToken);
        state.put("status",        "EXECUTING");
        state.put("startTime",     String.valueOf(startTime));
        state.put("timeoutMs",     String.valueOf(timeoutMs));
        state.put("deviceId",      "");
        state.put("taskId",        "");
        state.put("output",        "");
        state.put("rawResponse",   "");
        state.put("errorCode",     "");
        state.put("errorMsg",      "");
        state.put("executeMs",     "0");
        state.put("params",        toJson(reqVO.getParams()));
        redisTemplate.opsForHash().putAll(redisKey, state);
        redisTemplate.expire(redisKey, EXPIRE_HOURS, TimeUnit.HOURS);

        // 2. 发送指令（指定设备 / 自动选择）
        DeviceSendResult sendResult;
        try {
            if (StringUtils.hasText(reqVO.getDeviceId())) {
                // 指定设备：跳过选择器，直接发往目标设备
                sendResult = deviceGateway.sendCommandToDevice(
                        reqVO.getDeviceId(),
                        reqVO.getCommandCode(),
                        safeParams(reqVO.getParams()),
                        execId, NODE_ID, callbackToken,
                        reqVO.isForceExec());
            } else {
                // 自动选择：从同类型空闲设备中择优（forceExec=true 时含 BUSY 设备）
                sendResult = deviceGateway.sendCommand(
                        reqVO.getDeviceType(),
                        reqVO.getCommandCode(),
                        safeParams(reqVO.getParams()),
                        execId, NODE_ID, callbackToken,
                        reqVO.isForceExec());
            }
        } catch (Exception e) {
            log.error("[DeviceDirectExec] 指令发送失败 execId={} error={}", execId, e.getMessage(), e);
            redisTemplate.opsForHash().putAll(redisKey, Map.of(
                    "status",    "FAILED",
                    "errorCode", "SEND_ERROR",
                    "errorMsg",  e.getMessage() != null ? e.getMessage() : "指令发送失败"
            ));
            DeviceCommandExecuteRespVO resp = new DeviceCommandExecuteRespVO();
            resp.setExecId(execId);
            resp.setStatus("FAILED");
            resp.setErrorCode("SEND_ERROR");
            resp.setErrorMsg(e.getMessage());
            return resp;
        }

        String taskId = sendResult.getTaskId();

        // 3. sendCommand 执行后 stateCache 已写入反向索引，从中取出实际 deviceId
        String actualDeviceId = stateCache.getDeviceIdByExecutionNode(execId, NODE_ID);
        redisTemplate.opsForHash().putAll(redisKey, Map.of(
                "taskId",   taskId != null ? taskId : "",
                "deviceId", actualDeviceId != null ? actualDeviceId : ""
        ));

        log.info("[DeviceDirectExec] 指令已发送 execId={} deviceId={} commandCode={} taskId={}",
                execId, actualDeviceId, reqVO.getCommandCode(), taskId);

        DeviceCommandExecuteRespVO resp = new DeviceCommandExecuteRespVO();
        resp.setExecId(execId);
        resp.setStatus("EXECUTING");
        resp.setDeviceId(actualDeviceId);
        resp.setTaskId(taskId);
        resp.setCallbackToken(callbackToken);
        return resp;
    }

    // ----------------------------------------------------------------
    //  查询结果（支持轮询）
    // ----------------------------------------------------------------

    @Override
    public DeviceCommandExecuteRespVO getResult(String execId) {
        String redisKey = redisKey(execId);
        Map<Object, Object> state = redisTemplate.opsForHash().entries(redisKey);

        if (state.isEmpty()) {
            DeviceCommandExecuteRespVO resp = new DeviceCommandExecuteRespVO();
            resp.setExecId(execId);
            resp.setStatus("NOT_FOUND");
            resp.setErrorMsg("执行记录不存在或已过期（最长保留 2 小时）");
            return resp;
        }

        // 惰性超时检测：EXECUTING 状态下判断是否已超时
        String status = str(state, "status");
        if ("EXECUTING".equals(status)) {
            long startTime = parseLong(state, "startTime");
            long timeoutMs = parseLong(state, "timeoutMs");
            if (startTime > 0 && System.currentTimeMillis() - startTime > timeoutMs) {
                redisTemplate.opsForHash().putAll(redisKey, Map.of(
                        "status",    "TIMEOUT",
                        "errorCode", "EXEC_TIMEOUT",
                        "errorMsg",  "设备未在规定时间内回调，执行已超时"
                ));
                state.put("status",    "TIMEOUT");
                state.put("errorCode", "EXEC_TIMEOUT");
                state.put("errorMsg",  "设备未在规定时间内回调，执行已超时");
                status = "TIMEOUT";
            }
        }

        DeviceCommandExecuteRespVO resp = new DeviceCommandExecuteRespVO();
        resp.setExecId(execId);
        resp.setStatus(status);
        resp.setDeviceId(str(state, "deviceId"));
        resp.setTaskId(str(state, "taskId"));
        resp.setRawResponse(nullIfBlank(str(state, "rawResponse")));
        resp.setErrorCode(nullIfBlank(str(state, "errorCode")));
        resp.setErrorMsg(nullIfBlank(str(state, "errorMsg")));
        resp.setExecuteMs(parseLong(state, "executeMs"));

        String outputJson = str(state, "output");
        if (StringUtils.hasText(outputJson)) {
            resp.setOutput(fromJson(outputJson));
        }
        return resp;
    }

    // ----------------------------------------------------------------
    //  设备回调处理（由 DeviceCallbackHandler 路由调用）
    // ----------------------------------------------------------------

    @Override
    public void onCallback(String execId,
                           boolean success,
                           Map<String, Object> outputs,
                           String errorCode,
                           String errorMsg,
                           String rawResponse) {
        String redisKey = redisKey(execId);

        long executeMs = 0;
        Object startTimeObj = redisTemplate.opsForHash().get(redisKey, "startTime");
        if (startTimeObj instanceof String s) {
            try { executeMs = System.currentTimeMillis() - Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }

        Map<String, String> updates = new HashMap<>();
        updates.put("status",      success ? "SUCCESS" : "FAILED");
        updates.put("executeMs",   String.valueOf(executeMs));
        updates.put("errorCode",   errorCode   != null ? errorCode   : "");
        updates.put("errorMsg",    errorMsg    != null ? errorMsg    : "");
        updates.put("rawResponse", rawResponse != null ? rawResponse : "");
        updates.put("output",      toJson(outputs));
        redisTemplate.opsForHash().putAll(redisKey, updates);

        log.info("[DeviceDirectExec] 执行完成 execId={} success={} executeMs={}", execId, success, executeMs);
    }

    // ----------------------------------------------------------------
    //  工具方法
    // ----------------------------------------------------------------

    private String redisKey(String execId) {
        return REDIS_PREFIX + execId;
    }

    private Map<String, Object> safeParams(Map<String, Object> params) {
        return params != null ? params : Map.of();
    }

    private String toJson(Object obj) {
        if (obj == null) return "";
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return ""; }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String str(Map<Object, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : "";
    }

    private long parseLong(Map<Object, Object> map, String key) {
        String v = str(map, key);
        if (!StringUtils.hasText(v)) return 0L;
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; }
    }

    private String nullIfBlank(String s) {
        return StringUtils.hasText(s) ? s : null;
    }

}
