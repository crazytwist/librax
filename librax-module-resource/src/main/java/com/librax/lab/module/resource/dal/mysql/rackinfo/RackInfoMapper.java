package com.librax.lab.module.resource.dal.mysql.rackinfo;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.resource.dal.dataobject.rackinfo.RackInfoDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.resource.controller.admin.rackinfo.vo.*;

/**
 * 货架/台面定义，库位的上级容器，归 resource 模块管理 Mapper
 *
 * @author 一南
 */
@Mapper
public interface RackInfoMapper extends BaseMapperX<RackInfoDO> {

    default PageResult<RackInfoDO> selectPage(RackInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RackInfoDO>()
                .eqIfPresent(RackInfoDO::getRackId, reqVO.getRackId())
                .likeIfPresent(RackInfoDO::getRackName, reqVO.getRackName())
                .eqIfPresent(RackInfoDO::getRackType, reqVO.getRackType())
                .eqIfPresent(RackInfoDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(RackInfoDO::getRowCount, reqVO.getRowCount())
                .eqIfPresent(RackInfoDO::getColCount, reqVO.getColCount())
                .eqIfPresent(RackInfoDO::getEnabled, reqVO.getEnabled())
                .betweenIfPresent(RackInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RackInfoDO::getId));
    }

}