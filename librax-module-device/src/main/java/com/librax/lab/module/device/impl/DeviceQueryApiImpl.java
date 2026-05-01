package com.librax.lab.module.device.impl;

import com.librax.lab.module.device.api.DeviceHealthView;
import com.librax.lab.module.device.api.DeviceQueryApi;
import com.librax.lab.module.device.api.enums.DeviceHealthStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class DeviceQueryApiImpl implements DeviceQueryApi {
    @Override
    public List<String> listAvailable(String deviceType, String zoneCode) {
        log.info("[DeviceQueryApi][MOCK] listAvailable type={} zone={}", deviceType, zoneCode);
        return Collections.singletonList(deviceType + "-01");
    }

    @Override
    public DeviceHealthView getHealth(String deviceId) {
        return DeviceHealthView.builder()
                .deviceId(deviceId)
                .online(true)
                .healthStatus(DeviceHealthStatusEnum.HEALTHY)
                .lastHeartbeatAt(LocalDateTime.now())
                .build();
    }

    @Override
    public String getZoneCode(String deviceId) {
        return "ZONE-A";
    }
}
