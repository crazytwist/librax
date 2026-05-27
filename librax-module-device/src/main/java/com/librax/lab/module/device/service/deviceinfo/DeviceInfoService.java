package com.librax.lab.module.device.service.deviceinfo;

import java.util.*;

import com.librax.lab.module.device.controller.admin.deviceinfo.vo.DeviceInfoPageReqVO;
import com.librax.lab.module.device.controller.admin.deviceinfo.vo.DeviceInfoSaveReqVO;
import jakarta.validation.*;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 设备基本信息表，一行一台物理设备 [lab_device_] Service 接口
 *
 * @author 一南
 */
public interface DeviceInfoService {

    /**
     * 创建设备基本信息表，一行一台物理设备 [lab_device_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDeviceInfo(@Valid DeviceInfoSaveReqVO createReqVO);

    /**
     * 更新设备基本信息表，一行一台物理设备 [lab_device_]
     *
     * @param updateReqVO 更新信息
     */
    void updateDeviceInfo(@Valid DeviceInfoSaveReqVO updateReqVO);

    /**
     * 删除设备基本信息表，一行一台物理设备 [lab_device_]
     *
     * @param id 编号
     */
    void deleteDeviceInfo(Long id);

    /**
    * 批量删除设备基本信息表，一行一台物理设备 [lab_device_]
    *
    * @param ids 编号
    */
    void deleteDeviceInfoListByIds(List<Long> ids);

    /**
     * 获得设备基本信息表，一行一台物理设备 [lab_device_]
     *
     * @param id 编号
     * @return 设备基本信息表，一行一台物理设备 [lab_device_]
     */
    DeviceInfoDO getDeviceInfo(Long id);

    /**
     * 获得设备基本信息表，一行一台物理设备 [lab_device_]分页
     *
     * @param pageReqVO 分页查询
     * @return 设备基本信息表，一行一台物理设备 [lab_device_]分页
     */
    PageResult<DeviceInfoDO> getDeviceInfoPage(DeviceInfoPageReqVO pageReqVO);

    /**
     * 按业务设备ID查询
     *
     * @param deviceId 设备业务ID，如 PH-METER-01
     * @return 设备信息，不存在返回 null
     */
    DeviceInfoDO getByDeviceId(String deviceId);

}