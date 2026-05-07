package com.librax.lab.module.resource.dal.mysql.slotinfo;

import java.util.*;

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
                .eqIfPresent(SlotInfoDO::getEnabled, reqVO.getEnabled())
                .betweenIfPresent(SlotInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SlotInfoDO::getId));
    }

}