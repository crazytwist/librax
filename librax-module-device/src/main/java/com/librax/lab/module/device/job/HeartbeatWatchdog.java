package com.librax.lab.module.device.job;

import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.dal.mysql.devicecommand.DeviceCommandMapper;
import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;
import com.librax.lab.module.device.driver.DeviceDriver;
import com.librax.lab.module.device.driver.DeviceDriverFactory;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备心跳检测定时任务
 *
 * <p>定期向配置了心跳的设备发送心跳指令，根据响应更新设备在线状态。
 * 心跳间隔由 {@code DeviceInfoDO.heartbeatIntervalMs} 控制，
 * 心跳指令由 {@code DeviceInfoDO.heartbeatCommand} 指定。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatWatchdog {

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceCommandMapper commandMapper;
    private final DeviceDriverFactory driverFactory;
    private final DeviceStateCache stateCache;

    /** 每台设备上次心跳时间（用于控制心跳频率） */
    private final Map<String, LocalDateTime> lastHeartbeatMap = new ConcurrentHashMap<>();

    /**
     * 每 30 秒扫描一次，检查哪些设备需要发送心跳
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    public void checkHeartbeats() {
        List<DeviceInfoDO> allDevices = deviceInfoMapper.selectList();
        if (allDevices == null || allDevices.isEmpty()) {
            return;
        }

        for (DeviceInfoDO device : allDevices) {
            if (!Boolean.TRUE.equals(device.getEnabled())) {
                continue;
            }
            if (device.getHeartbeatIntervalMs() == null || device.getHeartbeatIntervalMs() <= 0) {
                continue;
            }
            if (device.getHeartbeatCommand() == null || device.getHeartbeatCommand().isEmpty()) {
                continue;
            }

            // 检查是否到了心跳时间
            String deviceId = device.getDeviceId();
            LocalDateTime lastBeat = lastHeartbeatMap.get(deviceId);
            long intervalMs = device.getHeartbeatIntervalMs();

            if (lastBeat != null) {
                long elapsed = java.time.Duration.between(lastBeat, LocalDateTime.now()).toMillis();
                if (elapsed < intervalMs) {
                    continue; // 还没到心跳时间
                }
            }

            // 执行心跳
            doHeartbeat(device);
        }
    }

    private void doHeartbeat(DeviceInfoDO device) {
        String deviceId = device.getDeviceId();
        try {
            // 加载心跳命令
            DeviceCommandDO heartbeatCmd = commandMapper.selectByTypeAndCode(
                    device.getDeviceType(), device.getHeartbeatCommand());
            if (heartbeatCmd == null) {
                log.warn("[HeartbeatWatchdog] 心跳命令不存在 deviceId={} cmd={}",
                        deviceId, device.getHeartbeatCommand());
                return;
            }

            // 获取驱动并发送
            DeviceDriver driver = driverFactory.getDriver(device.getProtocol());
            driver.send(device, heartbeatCmd, "{}", "HEARTBEAT", "");

            // 心跳成功
            lastHeartbeatMap.put(deviceId, LocalDateTime.now());

            // 如果设备之前是 OFFLINE，恢复为 IDLE
            if (stateCache.getStatus(deviceId).isAbnormal()) {
                log.info("[HeartbeatWatchdog] 设备恢复在线 deviceId={}", deviceId);
            }
            // 只有当设备不是 BUSY 时才标记为 IDLE（避免覆盖正在执行任务的状态）
            if (stateCache.isIdle(deviceId) || stateCache.getStatus(deviceId).isAbnormal()) {
                stateCache.markIdle(deviceId);
            }

            log.debug("[HeartbeatWatchdog] 心跳成功 deviceId={}", deviceId);

        } catch (Exception e) {
            log.warn("[HeartbeatWatchdog] 心跳失败 deviceId={}: {}", deviceId, e.getMessage());
            lastHeartbeatMap.put(deviceId, LocalDateTime.now()); // 记录时间避免频繁重试

            // 心跳失败 = 设备无响应，无论当前是 IDLE 还是 BUSY 都标记 OFFLINE
            // BUSY 设备心跳失败说明设备已挂，任务不会有回调，OFFLINE 后 DeviceSelector 自动跳过
            stateCache.markOffline(deviceId);
        }
    }
}
