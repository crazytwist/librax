package com.librax.lab.module.resource.dal.mysql.location;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.controller.admin.location.vo.LocationPageReqVO;
import com.librax.lab.module.resource.dal.dataobject.location.LocationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 区位信息 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface LocationMapper extends BaseMapperX<LocationDO> {

    default PageResult<LocationDO> selectPage(LocationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LocationDO>()
                .eqIfPresent(LocationDO::getCode, reqVO.getCode())
                .likeIfPresent(LocationDO::getName, reqVO.getName())
                .eqIfPresent(LocationDO::getParentId, reqVO.getParentId())
                .eqIfPresent(LocationDO::getLevel, reqVO.getLevel())
                .eqIfPresent(LocationDO::getType, reqVO.getType())
                .eqIfPresent(LocationDO::getCoordinates, reqVO.getCoordinates())
                .eqIfPresent(LocationDO::getStatus, reqVO.getStatus())
                .eqIfPresent(LocationDO::getPurpose, reqVO.getPurpose())
                .eqIfPresent(LocationDO::getRemark, reqVO.getRemark())
                .eqIfPresent(LocationDO::getExtData, reqVO.getExtData())
                .eqIfPresent(LocationDO::getExtField1, reqVO.getExtField1())
                .eqIfPresent(LocationDO::getExtField2, reqVO.getExtField2())
                .eqIfPresent(LocationDO::getExtField3, reqVO.getExtField3())
                .eqIfPresent(LocationDO::getExtField4, reqVO.getExtField4())
                .eqIfPresent(LocationDO::getExtField5, reqVO.getExtField5())
                .betweenIfPresent(LocationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(LocationDO::getId));
    }

}