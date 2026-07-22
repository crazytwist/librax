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

    default List<SlotInfoDO> selectEnabledByRackId(String rackId) {
        return selectList(new LambdaQueryWrapperX<SlotInfoDO>()
                .eq(SlotInfoDO::getRackId, rackId)
                .eq(SlotInfoDO::getEnabled, true)
                .orderByAsc(SlotInfoDO::getPositionLayer)
                .orderByAsc(SlotInfoDO::getPositionRow)
                .orderByAsc(SlotInfoDO::getPositionCol)
                .orderByAsc(SlotInfoDO::getSlotId));
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

    /** 原子预留一个空库位，防止并发搬运任务重复选择。 */
    default int reserveIfEmpty(String slotId) {
        return update(null, new LambdaUpdateWrapper<SlotInfoDO>()
                .eq(SlotInfoDO::getSlotId, slotId)
                .eq(SlotInfoDO::getEnabled, true)
                .eq(SlotInfoDO::getStatus, "EMPTY")
                .lt(SlotInfoDO::getCurrentCount, 1)
                .set(SlotInfoDO::getStatus, "RESERVED")
                .set(SlotInfoDO::getUpdateTime, LocalDateTime.now()));
    }

    /** 将预留库位提交为实际占用。 */
    default int occupyReserved(String slotId, String instanceId) {
        return update(null, new LambdaUpdateWrapper<SlotInfoDO>()
                .eq(SlotInfoDO::getSlotId, slotId)
                .eq(SlotInfoDO::getStatus, "RESERVED")
                .set(SlotInfoDO::getStatus, "OCCUPIED")
                .set(SlotInfoDO::getCurrentCount, 1)
                .set(SlotInfoDO::getOccupiedBy, instanceId)
                .set(SlotInfoDO::getUpdateTime, LocalDateTime.now()));
    }

    /** 上游设备通知补料完成：只允许把空库位原子更新为已占用。 */
    default int markReadyIfEmpty(String slotId, String instanceId) {
        return update(null, new LambdaUpdateWrapper<SlotInfoDO>()
                .eq(SlotInfoDO::getSlotId, slotId)
                .eq(SlotInfoDO::getEnabled, true)
                .eq(SlotInfoDO::getStatus, "EMPTY")
                .eq(SlotInfoDO::getCurrentCount, 0)
                .set(SlotInfoDO::getStatus, "OCCUPIED")
                .set(SlotInfoDO::getCurrentCount, 1)
                .set(SlotInfoDO::getOccupiedBy, instanceId)
                .set(SlotInfoDO::getUpdateTime, LocalDateTime.now()));
    }

    /** 仓储机械臂完成备料：将服务端预留的中转位提交为实际占用。 */
    default int markReadyIfReserved(String slotId, String instanceId) {
        return update(null, new LambdaUpdateWrapper<SlotInfoDO>()
                .eq(SlotInfoDO::getSlotId, slotId)
                .eq(SlotInfoDO::getEnabled, true)
                .eq(SlotInfoDO::getStatus, "RESERVED")
                .set(SlotInfoDO::getStatus, "OCCUPIED")
                .set(SlotInfoDO::getCurrentCount, 1)
                .set(SlotInfoDO::getOccupiedBy, instanceId)
                .set(SlotInfoDO::getUpdateTime, LocalDateTime.now()));
    }

    /** 释放空库位或本次任务的预留。 */
    default int clearOccupancy(String slotId) {
        return update(null, new LambdaUpdateWrapper<SlotInfoDO>()
                .eq(SlotInfoDO::getSlotId, slotId)
                .set(SlotInfoDO::getStatus, "EMPTY")
                .set(SlotInfoDO::getCurrentCount, 0)
                .set(SlotInfoDO::getOccupiedBy, null)
                .set(SlotInfoDO::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 查找指定区域内可用于入库的空闲仓储库位（下料回库用）。
     * slot_usage 匹配物料 typeCode，或 slot_usage 为 NULL（通用位）。
     * 按层/行/列升序排列，保证分配顺序稳定。
     *
     * @param zoneCode  仓储区域编码
     * @param slotUsage 物料容器类型编码（对应 lab_container_type.type_code）
     */
    default List<SlotInfoDO> selectEmptyWarehouseSlots(String zoneCode, String slotUsage) {
        return selectList(new LambdaQueryWrapperX<SlotInfoDO>()
                .eq(SlotInfoDO::getZoneCode, zoneCode)
                .eq(SlotInfoDO::getEnabled, true)
                .eq(SlotInfoDO::getStatus, "EMPTY")
                .and(w -> w.eq(SlotInfoDO::getSlotUsage, slotUsage)
                           .or().isNull(SlotInfoDO::getSlotUsage))
                .orderByAsc(SlotInfoDO::getPositionLayer)
                .orderByAsc(SlotInfoDO::getPositionRow)
                .orderByAsc(SlotInfoDO::getPositionCol)
                .orderByAsc(SlotInfoDO::getSlotId));
    }

    /**
     * 查找指定货架内按内容类型匹配的空闲库位（补料目标位分配用）。
     * slot_usage 匹配物料 contentType，或 slot_usage 为 NULL（通用位）。
     * 按层/行/列升序排列，保证分配顺序稳定。
     *
     * @param rackId    目标货架编码
     * @param slotUsage 物料内容类型（对应 lab_material_instance.content_type）
     */
    /**
     * 查找指定货架内按物料编码匹配的空闲库位（补料目标位分配用）。
     *
     * <p>匹配规则：
     * <ul>
     *   <li>slotUsage 有值 → slot_usage = slotUsage OR slot_usage IS NULL（通用位）</li>
     *   <li>slotUsage 为空 → 不限 slot_usage，返回所有空位（调用方无物料编码时的兜底）</li>
     * </ul>
     *
     * @param rackId    目标货架编码
     * @param slotUsage 物料编码（对应 lab_material_instance.material_code），为空则不过滤
     */
    default List<SlotInfoDO> selectEmptySlotsByRack(String rackId, String slotUsage) {
        LambdaQueryWrapperX<SlotInfoDO> wrapper = new LambdaQueryWrapperX<SlotInfoDO>()
                .eq(SlotInfoDO::getRackId, rackId)
                .eq(SlotInfoDO::getEnabled, true)
                .eq(SlotInfoDO::getStatus, "EMPTY");
        // slotUsage 有值才加类型过滤，为空则不限（兜底返回所有空位）
        if (org.springframework.util.StringUtils.hasText(slotUsage)) {
            wrapper.and(w -> w.eq(SlotInfoDO::getSlotUsage, slotUsage)
                              .or().isNull(SlotInfoDO::getSlotUsage));
        }
        return selectList(wrapper
                .orderByAsc(SlotInfoDO::getPositionLayer)
                .orderByAsc(SlotInfoDO::getPositionRow)
                .orderByAsc(SlotInfoDO::getPositionCol)
                .orderByAsc(SlotInfoDO::getSlotId));
    }

}
