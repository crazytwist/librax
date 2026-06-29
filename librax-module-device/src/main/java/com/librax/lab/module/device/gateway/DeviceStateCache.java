package com.librax.lab.module.device.gateway;

import com.librax.lab.module.device.enums.DeviceStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStateCache {

    private final StringRedisTemplate redisTemplate;

    // device:state:{deviceId}  → Hash: status/taskId/executionId/busySince
    private static final String STATE_PREFIX = "device:state:";
    // device:exec:{executionId}:{nodeId} → deviceId（反向索引，O(1)释放用）
    private static final String EXEC_PREFIX  = "device:exec:";
    private static final long   EXPIRE_HOURS = 24;

    // ----------------------------------------------------------------
    // 写操作
    // ----------------------------------------------------------------

    /**
     * 标记设备忙碌，同时写入反向索引
     */
    public void markBusy(String deviceId,
                         String taskId,
                         String executionId,
                         String nodeId) {
        // 1. 设备状态
        String stateKey = stateKey(deviceId);
        redisTemplate.opsForHash().putAll(stateKey, Map.of(
                "status",      "BUSY",
                "taskId",      taskId,
                "executionId", executionId,
                "nodeId",      nodeId,
                "busySince",   LocalDateTime.now().toString()
        ));
        redisTemplate.expire(stateKey, EXPIRE_HOURS, TimeUnit.HOURS);

        // 2. 反向索引：executionId + nodeId → deviceId
        String execKey = execKey(executionId, nodeId);
        redisTemplate.opsForValue().set(execKey, deviceId, EXPIRE_HOURS, TimeUnit.HOURS);

        log.debug("[DeviceStateCache] 标记忙碌 deviceId={} executionId={} nodeId={}",
                deviceId, executionId, nodeId);
    }

    /**
     * 标记设备空闲
     */
    public void markIdle(String deviceId) {
        String stateKey = stateKey(deviceId);

        // 取出旧的 executionId/nodeId，用于删除反向索引
        Map<Object, Object> old = redisTemplate.opsForHash().entries(stateKey);
        String oldExecId = (String) old.get("executionId");
        String oldNodeId = (String) old.get("nodeId");
        if (oldExecId != null && oldNodeId != null) {
            redisTemplate.delete(execKey(oldExecId, oldNodeId));
        }

        redisTemplate.opsForHash().putAll(stateKey, Map.of(
                "status",      "IDLE",
                "taskId",      "",
                "executionId", "",
                "nodeId",      ""
        ));
        redisTemplate.expire(stateKey, EXPIRE_HOURS, TimeUnit.HOURS);

        log.debug("[DeviceStateCache] 标记空闲 deviceId={}", deviceId);
    }

    /**
     * 通过反向索引，O(1) 找到设备并标记为 IDLE
     * CallbackDispatcher 回调时使用
     */
    public void markIdleByExecutionNode(String executionId, String nodeId) {
        String execKey = execKey(executionId, nodeId);
        String deviceId = redisTemplate.opsForValue().get(execKey);

        if (deviceId == null) {
            log.debug("[DeviceStateCache] 反向索引不存在（已释放或部分回调重入）executionId={} nodeId={}",
                    executionId, nodeId);
            return;
        }

        // 删除反向索引
        redisTemplate.delete(execKey);

        // 设备状态改回 IDLE
        redisTemplate.opsForHash().putAll(stateKey(deviceId), Map.of(
                "status",      "IDLE",
                "taskId",      "",
                "executionId", "",
                "nodeId",      ""
        ));

        log.info("[DeviceStateCache] 设备释放(反向索引) deviceId={} executionId={} nodeId={}",
                deviceId, executionId, nodeId);
    }

    public void markOffline(String deviceId) {
        redisTemplate.opsForHash().put(stateKey(deviceId), "status", "OFFLINE");
        log.info("[DeviceStateCache] 设备离线 deviceId={}", deviceId);
    }

    public void markFault(String deviceId, String reason) {
        redisTemplate.opsForHash().putAll(stateKey(deviceId), Map.of(
                "status", "FAULT",
                "faultReason", reason != null ? reason : ""
        ));
        log.warn("[DeviceStateCache] 设备故障 deviceId={} reason={}", deviceId, reason);
    }

    // ----------------------------------------------------------------
    // 读操作
    // ----------------------------------------------------------------

    public boolean isIdle(String deviceId) {
        Object status = redisTemplate.opsForHash().get(stateKey(deviceId), "status");
        return "IDLE".equals(status);
    }

    public DeviceStatusEnum getStatus(String deviceId) {
        Object status = redisTemplate.opsForHash().get(stateKey(deviceId), "status");
        if (status == null) return DeviceStatusEnum.OFFLINE;
        try {
            return DeviceStatusEnum.valueOf((String) status);
        } catch (IllegalArgumentException e) {
            return DeviceStatusEnum.OFFLINE;
        }
    }

    public String getTaskId(String deviceId) {
        Object taskId = redisTemplate.opsForHash().get(stateKey(deviceId), "taskId");
        return taskId != null ? (String) taskId : null;
    }

    /**
     * 通过反向索引查询设备ID（只读，不释放）
     * <p>
     * 供直接执行场景在 {@code sendCommand} 后回查实际分配的 deviceId。
     */
    public String getDeviceIdByExecutionNode(String executionId, String nodeId) {
        return redisTemplate.opsForValue().get(execKey(executionId, nodeId));
    }


    // ----------------------------------------------------------------
    // Key 工具
    // ----------------------------------------------------------------

    private String stateKey(String deviceId) {
        return STATE_PREFIX + deviceId;
    }

    private String execKey(String executionId, String nodeId) {
        return EXEC_PREFIX + executionId + ":" + nodeId;
    }
}