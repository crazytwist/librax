package com.librax.lab.module.device.job;

import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时将所有启用设备的 Redis 状态初始化为 IDLE（若尚无状态记录）。
 *
 * <p>解决问题：设备记录在 DB 中 enabled=true，但 Redis 无对应状态 key，
 * 导致 DeviceStateCache.getStatus() 返回 OFFLINE，ResourceSelector 过滤后
 * 候选列表为空，ResourcePool 报 NO_HEALTHY 错误。
 *
 * <p>逻辑：
 * <ul>
 *   <li>若 Redis 已有状态（IDLE/BUSY/FAULT/OFFLINE），不覆盖</li>
 *   <li>若 Redis 无状态，则以 DB 中 status 字段为准：ONLINE → IDLE；其他 → OFFLINE</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStateInitializer implements ApplicationRunner {

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceStateCache stateCache;

    @Override
    public void run(ApplicationArguments args) {
        List<DeviceInfoDO> devices = deviceInfoMapper.selectList();
        if (devices == null || devices.isEmpty()) {
            return;
        }

        int initialized = 0;
        for (DeviceInfoDO device : devices) {
            if (!Boolean.TRUE.equals(device.getEnabled())) {
                continue;
            }
            String deviceId = device.getDeviceId();

            // 已有 Redis 状态记录的设备（包括 BUSY/FAULT/OFFLINE）不覆盖
            if (stateCache.hasState(deviceId)) {
                continue;
            }

            // 参考 DB 初始状态
            String dbStatus = device.getStatus();
            if ("ONLINE".equalsIgnoreCase(dbStatus) || "IDLE".equalsIgnoreCase(dbStatus)) {
                stateCache.markIdle(deviceId);
                log.info("[DeviceStateInitializer] 设备初始化为 IDLE deviceId={} dbStatus={}", deviceId, dbStatus);
                initialized++;
            } else {
                // OFFLINE/FAULT/null → 保持 OFFLINE，等待心跳恢复
                log.debug("[DeviceStateInitializer] 设备跳过初始化 deviceId={} dbStatus={}", deviceId, dbStatus);
            }
        }

        log.info("[DeviceStateInitializer] 完成，共初始化 {} 台设备为 IDLE", initialized);
    }
}
