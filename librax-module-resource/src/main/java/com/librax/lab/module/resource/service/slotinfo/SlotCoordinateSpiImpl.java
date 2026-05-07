package com.librax.lab.module.resource.service.slotinfo;

import com.librax.lab.module.flow.api.slot.SlotCoordinate;
import com.librax.lab.module.flow.api.slot.SlotCoordinateSpi;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.dal.mysql.slotinfo.SlotInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库位坐标解析实现
 * <p>
 * 从 lab_slot_info 表查询库位的世界坐标信息，
 * 供 AGV 执行器和机械臂驱动使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotCoordinateSpiImpl implements SlotCoordinateSpi {

    private final SlotInfoMapper slotInfoMapper;

    @Override
    public SlotCoordinate resolve(String slotId) {
        if (slotId == null || slotId.isEmpty()) {
            return null;
        }

        SlotInfoDO slot = slotInfoMapper.selectBySlotId(slotId);
        if (slot == null) {
            log.warn("[SlotCoordinate] 库位不存在 slotId={}", slotId);
            return null;
        }

        if (!Boolean.TRUE.equals(slot.getEnabled())) {
            log.warn("[SlotCoordinate] 库位已禁用 slotId={}", slotId);
            return null;
        }

        return SlotCoordinate.builder()
                .slotId(slot.getSlotId())
                .slotName(slot.getSlotName())
                .slotType(slot.getSlotType())
                .zoneCode(slot.getZoneCode())
                .coordX(slot.getCoordX())
                .coordY(slot.getCoordY())
                .coordZ(slot.getCoordZ())
                .ownerId(slot.getOwnerId())
                .localIndex(slot.getLocalIndex())
                .build();
    }
}
