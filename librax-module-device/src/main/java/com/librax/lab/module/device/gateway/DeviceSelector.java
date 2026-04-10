package com.librax.lab.module.device.gateway;


import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeviceSelector {

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceStateCache stateCache;

    /**
     * 选择一台空闲设备（优先同区域，同区域内轮询）
     */
    public DeviceInfoDO select(String deviceType) {
        List<DeviceInfoDO> candidates = deviceInfoMapper
                .selectEnabledByType(deviceType);

        return candidates.stream()
                .filter(d -> stateCache.isIdle(d.getDeviceId()))
                .findFirst()
                .orElse(null);
    }
}