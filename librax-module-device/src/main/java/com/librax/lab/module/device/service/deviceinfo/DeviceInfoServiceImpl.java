package com.librax.lab.module.device.service.deviceinfo;

import com.librax.lab.module.device.controller.admin.deviceinfo.vo.DeviceInfoPageReqVO;
import com.librax.lab.module.device.controller.admin.deviceinfo.vo.DeviceInfoSaveReqVO;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.deviceinfo.vo.*;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.device.dal.mysql.deviceinfo.DeviceInfoMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.module.device.enums.ErrorCodeConstants.*;

/**
 * 设备基本信息表，一行一台物理设备 [lab_device_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class DeviceInfoServiceImpl implements DeviceInfoService {

    @Resource
    private DeviceInfoMapper deviceInfoMapper;

    @Override
    public Long createDeviceInfo(DeviceInfoSaveReqVO createReqVO) {
        // 插入
        DeviceInfoDO deviceInfo = BeanUtils.toBean(createReqVO, DeviceInfoDO.class);
        deviceInfoMapper.insert(deviceInfo);

        // 返回
        return deviceInfo.getId();
    }

    @Override
    public void updateDeviceInfo(DeviceInfoSaveReqVO updateReqVO) {
        // 校验存在
        validateDeviceInfoExists(updateReqVO.getId());
        // 更新
        DeviceInfoDO updateObj = BeanUtils.toBean(updateReqVO, DeviceInfoDO.class);
        deviceInfoMapper.updateById(updateObj);
    }

    @Override
    public void deleteDeviceInfo(Long id) {
        // 校验存在
        validateDeviceInfoExists(id);
        // 删除
        deviceInfoMapper.deleteById(id);
    }

    @Override
        public void deleteDeviceInfoListByIds(List<Long> ids) {
        // 删除
        deviceInfoMapper.deleteByIds(ids);
        }


    private void validateDeviceInfoExists(Long id) {
        if (deviceInfoMapper.selectById(id) == null) {
            throw exception(DEVICE_INFO_NOT_EXISTS);
        }
    }

    @Override
    public DeviceInfoDO getDeviceInfo(Long id) {
        return deviceInfoMapper.selectById(id);
    }

    @Override
    public PageResult<DeviceInfoDO> getDeviceInfoPage(DeviceInfoPageReqVO pageReqVO) {
        return deviceInfoMapper.selectPage(pageReqVO);
    }

}