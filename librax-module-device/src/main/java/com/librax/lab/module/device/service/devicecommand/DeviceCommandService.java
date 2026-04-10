package com.librax.lab.module.device.service.devicecommand;

import java.util.*;

import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandPageReqVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandSaveReqVO;
import jakarta.validation.*;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_] Service 接口
 *
 * @author 一南
 */
public interface DeviceCommandService {

    /**
     * 创建设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDeviceCommand(@Valid DeviceCommandSaveReqVO createReqVO);

    /**
     * 更新设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]
     *
     * @param updateReqVO 更新信息
     */
    void updateDeviceCommand(@Valid DeviceCommandSaveReqVO updateReqVO);

    /**
     * 删除设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]
     *
     * @param id 编号
     */
    void deleteDeviceCommand(Long id);

    /**
    * 批量删除设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]
    *
    * @param ids 编号
    */
    void deleteDeviceCommandListByIds(List<Long> ids);

    /**
     * 获得设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]
     *
     * @param id 编号
     * @return 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]
     */
    DeviceCommandDO getDeviceCommand(Long id);

    /**
     * 获得设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]分页
     *
     * @param pageReqVO 分页查询
     * @return 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]分页
     */
    PageResult<DeviceCommandDO> getDeviceCommandPage(DeviceCommandPageReqVO pageReqVO);

}