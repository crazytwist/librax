package com.librax.lab.module.lab.dal.mysql.containertype;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.containertype.ContainerTypeDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.containertype.vo.*;

/**
 * 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContainerTypeMapper extends BaseMapperX<ContainerTypeDO> {

    /** 按类型编码批量查询，用于给物料实例列表回填 typeName（冗余展示字段，不落库）。 */
    default List<ContainerTypeDO> selectListByTypeCodes(Collection<String> typeCodes) {
        if (typeCodes == null || typeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ContainerTypeDO>()
                .in(ContainerTypeDO::getTypeCode, typeCodes));
    }

    default PageResult<ContainerTypeDO> selectPage(ContainerTypePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContainerTypeDO>()
                .likeIfPresent(ContainerTypeDO::getTypeCode, reqVO.getTypeCode())
                .likeIfPresent(ContainerTypeDO::getTypeName, reqVO.getTypeName())
                .eqIfPresent(ContainerTypeDO::getContainerType, reqVO.getContainerType())
                .eqIfPresent(ContainerTypeDO::getHierarchyRole, reqVO.getHierarchyRole())
                .eqIfPresent(ContainerTypeDO::getMaxVolUl, reqVO.getMaxVolUl())
                .eqIfPresent(ContainerTypeDO::getEnabled, reqVO.getEnabled())
                .betweenIfPresent(ContainerTypeDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContainerTypeDO::getId));
    }

}