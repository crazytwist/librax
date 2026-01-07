package com.librax.lab.module.resource.dal.mysql.material;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.controller.admin.material.vo.MaterialPageReqVO;
import com.librax.lab.module.resource.dal.dataobject.material.MaterialDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物料基础信息 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MaterialMapper extends BaseMapperX<MaterialDO> {

    default PageResult<MaterialDO> selectPage(MaterialPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MaterialDO>()
                .eqIfPresent(MaterialDO::getCode, reqVO.getCode())
                .likeIfPresent(MaterialDO::getName, reqVO.getName())
                .eqIfPresent(MaterialDO::getType, reqVO.getType())
                .eqIfPresent(MaterialDO::getCategory, reqVO.getCategory())
                .eqIfPresent(MaterialDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MaterialDO::getArea, reqVO.getArea())
                .eqIfPresent(MaterialDO::getLocation, reqVO.getLocation())
                .eqIfPresent(MaterialDO::getExtData, reqVO.getExtData())
                .eqIfPresent(MaterialDO::getExtField1, reqVO.getExtField1())
                .eqIfPresent(MaterialDO::getExtField2, reqVO.getExtField2())
                .eqIfPresent(MaterialDO::getExtField3, reqVO.getExtField3())
                .eqIfPresent(MaterialDO::getExtField4, reqVO.getExtField4())
                .eqIfPresent(MaterialDO::getExtField5, reqVO.getExtField5())
                .eqIfPresent(MaterialDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(MaterialDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MaterialDO::getId));
    }

}