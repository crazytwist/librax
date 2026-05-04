package com.librax.lab.module.resource.dal.mysql.resourceconfig;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.resource.dal.dataobject.resourceconfig.ResourceConfigDO;
import com.librax.lab.module.resource.enums.OwnershipTypeEnum;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.resource.controller.admin.resourceconfig.vo.*;

/**
 * 资源配置表,运行时锁状态见Redis Mapper
 *
 * @author 一南
 */
@Mapper
public interface ResourceConfigMapper extends BaseMapperX<ResourceConfigDO> {

    default PageResult<ResourceConfigDO> selectPage(ResourceConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ResourceConfigDO>()
                .eqIfPresent(ResourceConfigDO::getResourceId, reqVO.getResourceId())
                .eqIfPresent(ResourceConfigDO::getResourceType, reqVO.getResourceType())
                .eqIfPresent(ResourceConfigDO::getOwnershipType, reqVO.getOwnershipType())
                .eqIfPresent(ResourceConfigDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(ResourceConfigDO::getEnabled, reqVO.getEnabled())
                .betweenIfPresent(ResourceConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ResourceConfigDO::getId));
    }

    /** 按类型 + 区域查所有启用的独占资源(严格本区) */
    default List<ResourceConfigDO> selectExclusiveByTypeAndZone(String resourceType, String zoneCode) {
        return selectList(new LambdaQueryWrapper<ResourceConfigDO>()
                .eq(ResourceConfigDO::getResourceType, resourceType)
                .eq(ResourceConfigDO::getOwnershipType, OwnershipTypeEnum.EXCLUSIVE.name())
                .eq(ResourceConfigDO::getZoneCode, zoneCode)
                .eq(ResourceConfigDO::getEnabled, true));
    }

    /** 按类型查所有启用的共享资源(不分区) */
    default List<ResourceConfigDO> selectSharedByType(String resourceType) {
        return selectList(new LambdaQueryWrapper<ResourceConfigDO>()
                .eq(ResourceConfigDO::getResourceType, resourceType)
                .eq(ResourceConfigDO::getOwnershipType, OwnershipTypeEnum.SHARED.name())
                .eq(ResourceConfigDO::getEnabled, true));
    }

    /** 查单个资源配置 */
    default ResourceConfigDO selectByResourceId(String resourceId) {
        return selectOne(ResourceConfigDO::getResourceId, resourceId);
    }

}