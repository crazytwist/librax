package com.librax.lab.module.resource.service.zonequota;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.resource.controller.admin.zonequota.vo.*;
import com.librax.lab.module.resource.dal.dataobject.zonequota.ZoneQuotaDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.resource.dal.mysql.zonequota.ZoneQuotaMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.*;

/**
 * 区域对共享资源的配额 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ZoneQuotaServiceImpl implements ZoneQuotaService {

    @Resource
    private ZoneQuotaMapper zoneQuotaMapper;

    @Override
    public Long createZoneQuota(ZoneQuotaSaveReqVO createReqVO) {
        // 插入
        ZoneQuotaDO zoneQuota = BeanUtils.toBean(createReqVO, ZoneQuotaDO.class);
        zoneQuotaMapper.insert(zoneQuota);

        // 返回
        return zoneQuota.getId();
    }

    @Override
    public void updateZoneQuota(ZoneQuotaSaveReqVO updateReqVO) {
        // 校验存在
        validateZoneQuotaExists(updateReqVO.getId());
        // 更新
        ZoneQuotaDO updateObj = BeanUtils.toBean(updateReqVO, ZoneQuotaDO.class);
        zoneQuotaMapper.updateById(updateObj);
    }

    @Override
    public void deleteZoneQuota(Long id) {
        // 校验存在
        validateZoneQuotaExists(id);
        // 删除
        zoneQuotaMapper.deleteById(id);
    }

    @Override
        public void deleteZoneQuotaListByIds(List<Long> ids) {
        // 删除
        zoneQuotaMapper.deleteByIds(ids);
        }


    private void validateZoneQuotaExists(Long id) {
        if (zoneQuotaMapper.selectById(id) == null) {
            throw exception(ZONE_QUOTA_NOT_EXISTS);
        }
    }

    @Override
    public ZoneQuotaDO getZoneQuota(Long id) {
        return zoneQuotaMapper.selectById(id);
    }

    @Override
    public PageResult<ZoneQuotaDO> getZoneQuotaPage(ZoneQuotaPageReqVO pageReqVO) {
        return zoneQuotaMapper.selectPage(pageReqVO);
    }

}