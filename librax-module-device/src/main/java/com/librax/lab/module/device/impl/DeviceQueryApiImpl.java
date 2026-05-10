package com.librax.lab.module.device.impl;

import com.librax.lab.module.device.api.DeviceHealthView;
import com.librax.lab.module.device.api.DeviceQueryApi;
import com.librax.lab.module.device.api.enums.DeviceHealthStatusEnum;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;
import com.librax.lab.module.device.enums.DeviceStatus;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceQueryApiImpl implements DeviceQueryApi {

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceStateCache stateCache;

    @Override
    public List<String> listAvailable(String deviceType, String zoneCode) {
        List<DeviceInfoDO> devices = deviceInfoMapper.selectEnabledByType(deviceType);
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyList();
        }

        return devices.stream()
                // 按区域过滤
                .filter(d -> zoneCode == null || zoneCode.equals(d.getZoneCode()))
                // 排除异常设备
                .filter(d -> !stateCache.getStatus(d.getDeviceId()).isAbnormal())
                .map(DeviceInfoDO::getDeviceId)
                .collect(Collectors.toList());
    }

    @Override
    public DeviceHealthView getHealth(String deviceId) {
        DeviceStatus status = stateCache.getStatus(deviceId);

        // 映射 DeviceStatus → DeviceHealthStatusEnum
        DeviceHealthStatusEnum healthStatus;
        boolean online;
        switch (status) {
            case IDLE:
            case BUSY:
                healthStatus = DeviceHealthStatusEnum.HEALTHY;
                online = true;
                break;
            case FAULT:
                healthStatus = DeviceHealthStatusEnum.FAULT;
                online = false;
                break;
            case OFFLINE:
            default:
                healthStatus = DeviceHealthStatusEnum.UNKNOWN;
                online = false;
                break;
        }

        return DeviceHealthView.builder()
                .deviceId(deviceId)
                .online(online)
                .healthStatus(DeviceHealthStatusEnum.HEALTHY)
                .build();
    }

    @Override
    public String getZoneCode(String deviceId) {
        DeviceInfoDO device = deviceInfoMapper.selectByDeviceId(deviceId);
        return device != null ? device.getZoneCode() : null;
    }
}
