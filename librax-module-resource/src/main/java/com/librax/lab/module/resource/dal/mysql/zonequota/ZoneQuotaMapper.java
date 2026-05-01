package com.librax.lab.module.resource.dal.mysql.zonequota;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.resource.dal.dataobject.zonequota.ZoneQuotaDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.resource.controller.admin.zonequota.vo.*;

/**
 * 区域对共享资源的配额 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ZoneQuotaMapper extends BaseMapperX<ZoneQuotaDO> {

    default PageResult<ZoneQuotaDO> selectPage(ZoneQuotaPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ZoneQuotaDO>()
                .eqIfPresent(ZoneQuotaDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(ZoneQuotaDO::getResourceType, reqVO.getResourceType())
                .betweenIfPresent(ZoneQuotaDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ZoneQuotaDO::getId));
    }

    /** 查某区域对某类共享资源的配额 */
    default ZoneQuotaDO selectByZoneAndType(String zoneCode, String resourceType) {
        return selectOne(new LambdaQueryWrapper<ZoneQuotaDO>()
                .eq(ZoneQuotaDO::getZoneCode, zoneCode)
                .eq(ZoneQuotaDO::getResourceType, resourceType));
    }

}