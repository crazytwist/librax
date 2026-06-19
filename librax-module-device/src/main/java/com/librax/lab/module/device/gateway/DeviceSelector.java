package com.librax.lab.module.device.gateway;


import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceSelector {

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceStateCache stateCache;

    /** round-robin 计数器 */
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * 全局跳过设备 IDLE 校验开关。
     * true  = 从所有启用设备中选择，忽略 BUSY 状态（设备不回调/调试场景）
     * false = 只选 IDLE 设备（生产默认）
     */
    @Value("${librax.device.skip-idle-check:false}")
    private boolean skipIdleCheck;

    /**
     * 选择一台空闲设备（不限区域）
     */
    public DeviceInfoDO select(String deviceType) {
        return select(deviceType, null, false);
    }

    public DeviceInfoDO select(String deviceType, String preferredZone) {
        return select(deviceType, preferredZone, false);
    }

    /**
     * 选择设备
     *
     * @param deviceType    设备类型
     * @param preferredZone 优先区域（可为 null）
     * @param forceExec     true 时跳过空闲过滤，从所有启用设备中选（调试用）
     * @return 目标设备，无可用时返回 null
     */
    public DeviceInfoDO select(String deviceType, String preferredZone, boolean forceExec) {
        List<DeviceInfoDO> candidates = deviceInfoMapper.selectEnabledByType(deviceType);

        // forceExec 或全局 skipIdleCheck 开启时跳过 IDLE 过滤
        boolean bypassIdle = forceExec || skipIdleCheck;
        if (skipIdleCheck) {
            log.debug("[DeviceSelector] skip-idle-check=true，跳过 BUSY 状态过滤 type={}", deviceType);
        }
        List<DeviceInfoDO> pool = bypassIdle
                ? candidates
                : candidates.stream()
                        .filter(d -> stateCache.isIdle(d.getDeviceId()))
                        .collect(Collectors.toList());

        if (pool.isEmpty()) {
            return null;
        }

        // 优先同区域
        if (preferredZone != null) {
            List<DeviceInfoDO> sameZone = pool.stream()
                    .filter(d -> preferredZone.equals(d.getZoneCode()))
                    .collect(Collectors.toList());
            if (!sameZone.isEmpty()) {
                pool = sameZone;
            }
        }

        int idx = Math.abs(counter.getAndIncrement() % pool.size());
        return pool.get(idx);
    }
}