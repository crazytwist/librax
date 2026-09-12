package com.librax.lab.module.lab.service.materialinstance;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.module.flow.api.material.MaterialLocationSpi;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.module.lab.dal.mysql.materialinstance.MaterialInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 独立仓储搬运时回写物料当前位置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialLocationSpiImpl implements MaterialLocationSpi {

    private final MaterialInstanceMapper instanceMapper;

    @Override
    public String getCurrentSlotId(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return null;
        }
        MaterialInstanceDO instance = instanceMapper.selectByInstanceId(instanceId);
        return instance == null ? null : instance.getSlotId();
    }

    @Override
    public boolean updateCurrentSlot(String instanceId, String slotId, String zoneCode) {
        if (instanceId == null || instanceId.isBlank()) {
            return false;
        }
        MaterialInstanceDO instance = instanceMapper.selectByInstanceId(instanceId);
        if (instance == null) {
            log.warn("[MaterialLocation] 物料不存在，跳过位置更新 instanceId={} slotId={}", instanceId, slotId);
            return false;
        }
        LambdaUpdateWrapper<MaterialInstanceDO> update = new LambdaUpdateWrapper<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getInstanceId, instanceId)
                .set(MaterialInstanceDO::getSlotId, slotId)
                .set(MaterialInstanceDO::getUpdateTime, LocalDateTime.now());
        if (slotId == null) {
            update.set(MaterialInstanceDO::getZoneCode, null);
        } else if (zoneCode != null) {
            update.set(MaterialInstanceDO::getZoneCode, zoneCode);
        }
        int rows = instanceMapper.update(null, update);
        log.info("[MaterialLocation] 更新物料位置 instanceId={} slotId={} zoneCode={} rows={}",
                instanceId, slotId, zoneCode, rows);
        return rows > 0;
    }
}
