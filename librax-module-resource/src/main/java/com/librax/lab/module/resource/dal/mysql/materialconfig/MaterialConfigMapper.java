package com.librax.lab.module.resource.dal.mysql.materialconfig;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.controller.admin.materialconfig.vo.MaterialConfigPageReqVO;
import com.librax.lab.module.resource.dal.dataobject.materialconfig.MaterialConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.*;



/**
 * 物料配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MaterialConfigMapper extends BaseMapperX<MaterialConfigDO> {

    default PageResult<MaterialConfigDO> selectPage(MaterialConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MaterialConfigDO>()
                .eqIfPresent(MaterialConfigDO::getType, reqVO.getType())
                .eqIfPresent(MaterialConfigDO::getTypePrefix, reqVO.getTypePrefix())
                .likeIfPresent(MaterialConfigDO::getTypeName, reqVO.getTypeName())
                .eqIfPresent(MaterialConfigDO::getIdPattern, reqVO.getIdPattern())
                .eqIfPresent(MaterialConfigDO::getNextSequence, reqVO.getNextSequence())
                .eqIfPresent(MaterialConfigDO::getRemark, reqVO.getRemark())
                .eqIfPresent(MaterialConfigDO::getExtData, reqVO.getExtData())
                .betweenIfPresent(MaterialConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MaterialConfigDO::getId));
    }

}