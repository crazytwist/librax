package com.librax.lab.module.device.gateway;


import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DeviceSelector {

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceStateCache stateCache;

    /** round-robin 计数器 */
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * 选择一台空闲设备（不限区域）
     */
    public DeviceInfoDO select(String deviceType) {
        return select(deviceType, null);
    }

    /**
     * 选择一台空闲设备（优先同区域，同区域内轮询）
     *
     * @param deviceType    设备类型
     * @param preferredZone 优先区域（可为 null）
     * @return 空闲设备，无可用时返回 null
     */
    public DeviceInfoDO select(String deviceType, String preferredZone) {
        List<DeviceInfoDO> candidates = deviceInfoMapper
                .selectEnabledByType(deviceType);

        // 过滤空闲设备
        List<DeviceInfoDO> idle = candidates.stream()
                .filter(d -> stateCache.isIdle(d.getDeviceId()))
                .collect(Collectors.toList());

        if (idle.isEmpty()) {
            return null;
        }

        // 优先同区域
        if (preferredZone != null) {
            List<DeviceInfoDO> sameZone = idle.stream()
                    .filter(d -> preferredZone.equals(d.getZoneCode()))
                    .collect(Collectors.toList());
            if (!sameZone.isEmpty()) {
                idle = sameZone;
            }
        }

        // round-robin 轮询
        int idx = Math.abs(counter.getAndIncrement() % idle.size());
        return idle.get(idx);
    }
}