package com.librax.lab.module.device.dal.mysql.deviceinfo;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.device.controller.admin.deviceinfo.vo.DeviceInfoPageReqVO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 设备基本信息表，一行一台物理设备 [lab_device_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface DeviceInfoMapper extends BaseMapperX<DeviceInfoDO> {

    default PageResult<DeviceInfoDO> selectPage(DeviceInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DeviceInfoDO>()
                .eqIfPresent(DeviceInfoDO::getDeviceId, reqVO.getDeviceId())
                .likeIfPresent(DeviceInfoDO::getDeviceName, reqVO.getDeviceName())
                .eqIfPresent(DeviceInfoDO::getDeviceType, reqVO.getDeviceType())
                .eqIfPresent(DeviceInfoDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(DeviceInfoDO::getStatus, reqVO.getStatus())
                .eqIfPresent(DeviceInfoDO::getEnabled, reqVO.getEnabled())
                .betweenIfPresent(DeviceInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DeviceInfoDO::getId));
    }

    default List<DeviceInfoDO> selectEnabledByType(String deviceType) {
        return selectList(DeviceInfoDO::getDeviceType, deviceType);
    }

    default DeviceInfoDO selectByDeviceId(String deviceId) {
        return selectOne(DeviceInfoDO::getDeviceId, deviceId);
    }
}