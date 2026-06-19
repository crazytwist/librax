package com.librax.lab.module.lab.dal.mysql.materialinstance;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import org.apache.ibatis.annotations.Mapper;
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
        return selectList(new LambdaQueryWrapperX<MaterialInstanceDO>()
                .eqIfPresent(MaterialInstanceDO::getMaterialCode, materialCode)
                .eqIfPresent(MaterialInstanceDO::getContentType, contentType)
                .eqIfPresent(MaterialInstanceDO::getZoneCode, zoneCode)
                .eq(MaterialInstanceDO::getStatus, "AVAILABLE")
                .and(w -> w.isNull(MaterialInstanceDO::getExpiredAt)
                        .or()
                        .ge(MaterialInstanceDO::getExpiredAt, java.time.LocalDate.now())));
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