package com.librax.lab.module.resource.service.location;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.resource.controller.admin.location.vo.LocationPageReqVO;
import com.librax.lab.module.resource.controller.admin.location.vo.LocationSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.location.LocationDO;
import com.librax.lab.module.resource.dal.mysql.location.LocationMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.LOCATION_NOT_EXISTS;

/**
 * 区位信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class LocationServiceImpl implements LocationService {

    @Resource
    private LocationMapper locationMapper;

    @Override
    public Long createLocation(LocationSaveReqVO createReqVO) {
        // 插入
        LocationDO location = BeanUtils.toBean(createReqVO, LocationDO.class);
        locationMapper.insert(location);

        // 返回
        return location.getId();
    }

    @Override
    public void updateLocation(LocationSaveReqVO updateReqVO) {
        // 校验存在
        validateLocationExists(updateReqVO.getId());
        // 更新
        LocationDO updateObj = BeanUtils.toBean(updateReqVO, LocationDO.class);
        locationMapper.updateById(updateObj);
    }

    @Override
    public void deleteLocation(Long id) {
        // 校验存在
        validateLocationExists(id);
        // 删除
        locationMapper.deleteById(id);
    }

    @Override
        public void deleteLocationListByIds(List<Long> ids) {
        // 删除
        locationMapper.deleteByIds(ids);
        }


    private void validateLocationExists(Long id) {
        if (locationMapper.selectById(id) == null) {
            throw exception(LOCATION_NOT_EXISTS);
        }
    }

    @Override
    public LocationDO getLocation(Long id) {
        return locationMapper.selectById(id);
    }

    @Override
    public PageResult<LocationDO> getLocationPage(LocationPageReqVO pageReqVO) {
        return locationMapper.selectPage(pageReqVO);
    }

}