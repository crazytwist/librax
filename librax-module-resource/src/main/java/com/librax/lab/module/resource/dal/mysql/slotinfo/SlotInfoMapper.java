package com.librax.lab.module.resource.dal.mysql.slotinfo;

import java.time.LocalDateTime;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.resource.controller.admin.slotinfo.vo.*;

/**
 * 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理 Mapper
 *
 * @author 一南
 */
@Mapper
public interface SlotInfoMapper extends BaseMapperX<SlotInfoDO> {

    default SlotInfoDO selectBySlotId(String slotId) {
        return selectOne(new LambdaQueryWrapperX<SlotInfoDO>()
                .eq(SlotInfoDO::getSlotId, slotId));
    }

    default PageResult<SlotInfoDO> selectPage(SlotInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SlotInfoDO>()
                .eqIfPresent(SlotInfoDO::getSlotId, reqVO.getSlotId())
                .likeIfPresent(SlotInfoDO::getSlotName, reqVO.getSlotName())
                .eqIfPresent(SlotInfoDO::getSlotType, reqVO.getSlotType())
                .eqIfPresent(SlotInfoDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(SlotInfoDO::getRackId, reqVO.getRackId())
                .eqIfPresent(SlotInfoDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SlotInfoDO::getEnabled, reqVO.getEnabled())
                .betweenIfPresent(SlotInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SlotInfoDO::getId));
    }

    /**
     * 物料放入库位：currentCount +1，达到容量时 status → OCCUPIED
     */
    default int occupy(String slotId, String instanceId, int capacity) {
        return update(null, new LambdaUpdateWrapper<SlotInfoDO>()
                .eq(SlotInfoDO::getSlotId, slotId)
                .setSql("current_count = current_count + 1")
                .setSql("status = IF(current_count + 1 >= " + capacity + ", 'OCCUPIED', 'OCCUPIED')")
                .set(SlotInfoDO::getOccupiedBy, instanceId)
                .set(SlotInfoDO::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 物料移出库位：currentCount -1，降到 0 时 status → EMPTY，occupiedBy 清空
     */
    default int release(String slotId) {
        return update(null, new LambdaUpdateWrapper<SlotInfoDO>()
                .eq(SlotInfoDO::getSlotId, slotId)
                .setSql("current_count = GREATEST(current_count - 1, 0)")
                .setSql("status = IF(current_count - 1 <= 0, 'EMPTY', 'OCCUPIED')")
                .setSql("occupied_by = IF(current_count - 1 <= 0, NULL, occupied_by)")
                .set(SlotInfoDO::getUpdateTime, LocalDateTime.now()));
    }

}