package com.librax.lab.module.lab.dal.mysql.materialinstance;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.module.lab.controller.admin.materialinstance.vo.*;

/**
 * 物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MaterialInstanceMapper extends BaseMapperX<MaterialInstanceDO> {

    /**
     * 按物料编码 + 内容类型 + 区域查找可用实例
     * 条件：status=AVAILABLE, 未过期
     */
    default MaterialInstanceDO selectByInstanceId(String instanceId) {
        return selectOne(new LambdaQueryWrapperX<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getInstanceId, instanceId));
    }

    /**
     * 根据库位ID列表批量查询已绑定的物料实例（每个库位至多一条）
     */
    default List<MaterialInstanceDO> selectBySlotIds(List<String> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MaterialInstanceDO>()
                .in(MaterialInstanceDO::getSlotId, slotIds));
    }

    default List<MaterialInstanceDO> selectAvailable(String materialCode,
                                                      String contentType,
                                                      String zoneCode) {
        return selectAvailable(null, materialCode, contentType, zoneCode);
    }

    default List<MaterialInstanceDO> selectAvailable(String typeCode,
                                                      String materialCode,
                                                      String contentType,
                                                      String zoneCode) {
        return selectList(new LambdaQueryWrapperX<MaterialInstanceDO>()
                .eqIfPresent(MaterialInstanceDO::getTypeCode, typeCode)
                .eqIfPresent(MaterialInstanceDO::getMaterialCode, materialCode)
                .eqIfPresent(MaterialInstanceDO::getContentType, contentType)
                .eqIfPresent(MaterialInstanceDO::getZoneCode, zoneCode)
                .eq(MaterialInstanceDO::getStatus, "AVAILABLE")
                .and(w -> w.isNull(MaterialInstanceDO::getExpiredAt)
                        .or()
                        .ge(MaterialInstanceDO::getExpiredAt, java.time.LocalDate.now())));
    }

    /** 原子预留可用物料，避免两个补料单选中同一实例。 */
    default int reserveAvailable(String instanceId) {
        return update(null, new LambdaUpdateWrapper<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getInstanceId, instanceId)
                .eq(MaterialInstanceDO::getStatus, "AVAILABLE")
                .set(MaterialInstanceDO::getStatus, "RESERVED")
                .set(MaterialInstanceDO::getUpdateTime, java.time.LocalDateTime.now()));
    }

    /**
     * 搬运完成后提交物料的新位置，并将状态恢复为搬运前的原始状态。
     * <p>搬运不改变物料的语义状态：AVAILABLE→AVAILABLE（补料），USED→USED（下料）。
     */
    default int completeTransfer(String instanceId, String slotId, String zoneCode, String targetStatus) {
        return update(null, new LambdaUpdateWrapper<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getInstanceId, instanceId)
                .eq(MaterialInstanceDO::getStatus, "RESERVED")
                .set(MaterialInstanceDO::getSlotId, slotId)
                .set(MaterialInstanceDO::getZoneCode, zoneCode)
                .set(MaterialInstanceDO::getStatus, targetStatus)
                .set(MaterialInstanceDO::getUpdateTime, java.time.LocalDateTime.now()));
    }

    /** 补料到位后提交物料的新位置（快捷方法，恢复为 AVAILABLE）。 */
    default int completeReplenishment(String instanceId, String slotId, String zoneCode) {
        return completeTransfer(instanceId, slotId, zoneCode, "AVAILABLE");
    }

    /**
     * 搬运失败时将物料预留归还原始状态。
     */
    default int releaseReservationTo(String instanceId, String targetStatus) {
        return update(null, new LambdaUpdateWrapper<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getInstanceId, instanceId)
                .eq(MaterialInstanceDO::getStatus, "RESERVED")
                .set(MaterialInstanceDO::getStatus, targetStatus)
                .set(MaterialInstanceDO::getUpdateTime, java.time.LocalDateTime.now()));
    }

    /** 补料失败时归还尚未搬走的库存预留（恢复为 AVAILABLE）。 */
    default int releaseReservation(String instanceId) {
        return releaseReservationTo(instanceId, "AVAILABLE");
    }

    /** 下料前原子锁定已使用物料（USED → RESERVED），防并发下料任务重复选中。 */
    default int reserveUsed(String instanceId) {
        return update(null, new LambdaUpdateWrapper<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getInstanceId, instanceId)
                .eq(MaterialInstanceDO::getStatus, "USED")
                .set(MaterialInstanceDO::getStatus, "RESERVED")
                .set(MaterialInstanceDO::getUpdateTime, java.time.LocalDateTime.now()));
    }


    default PageResult<MaterialInstanceDO> selectPage(MaterialInstancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MaterialInstanceDO>()
                .eqIfPresent(MaterialInstanceDO::getInstanceId, reqVO.getInstanceId())
                .eqIfPresent(MaterialInstanceDO::getTypeCode, reqVO.getTypeCode())
                .eqIfPresent(MaterialInstanceDO::getBarcode, reqVO.getBarcode())
                .eqIfPresent(MaterialInstanceDO::getParentId, reqVO.getParentId())
                .eqIfPresent(MaterialInstanceDO::getSlotIndex, reqVO.getSlotIndex())
                .eqIfPresent(MaterialInstanceDO::getSlotId, reqVO.getSlotId())
                .eqIfPresent(MaterialInstanceDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(MaterialInstanceDO::getContentType, reqVO.getContentType())
                .eqIfPresent(MaterialInstanceDO::getMaterialCode, reqVO.getMaterialCode())
                .eqIfPresent(MaterialInstanceDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(MaterialInstanceDO::getLotNo, reqVO.getLotNo())
                .eqIfPresent(MaterialInstanceDO::getCurrentVolUl, reqVO.getCurrentVolUl())
                .eqIfPresent(MaterialInstanceDO::getConcentration, reqVO.getConcentration())
                .eqIfPresent(MaterialInstanceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MaterialInstanceDO::getReceivedAt, reqVO.getReceivedAt())
                .eqIfPresent(MaterialInstanceDO::getOpenedAt, reqVO.getOpenedAt())
                .eqIfPresent(MaterialInstanceDO::getExpiredAt, reqVO.getExpiredAt())
                .eqIfPresent(MaterialInstanceDO::getSourceExecutionId, reqVO.getSourceExecutionId())
                .eqIfPresent(MaterialInstanceDO::getSourceNodeId, reqVO.getSourceNodeId())
                .eqIfPresent(MaterialInstanceDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(MaterialInstanceDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MaterialInstanceDO::getId));
    }

}
