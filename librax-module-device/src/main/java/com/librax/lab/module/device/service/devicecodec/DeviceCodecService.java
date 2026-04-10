package com.librax.lab.module.device.service.devicecodec;

import java.util.*;

import com.librax.lab.module.device.controller.admin.devicecodec.vo.DeviceCodecPageReqVO;
import com.librax.lab.module.device.controller.admin.devicecodec.vo.DeviceCodecSaveReqVO;
import jakarta.validation.*;
import com.librax.lab.module.device.dal.dataobject.devicecodec.DeviceCodecDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_] Service 接口
 *
 * @author 一南
 */
public interface DeviceCodecService {

    /**
     * 创建设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDeviceCodec(@Valid DeviceCodecSaveReqVO createReqVO);

    /**
     * 更新设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]
     *
     * @param updateReqVO 更新信息
     */
    void updateDeviceCodec(@Valid DeviceCodecSaveReqVO updateReqVO);

    /**
     * 删除设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]
     *
     * @param id 编号
     */
    void deleteDeviceCodec(Long id);

    /**
    * 批量删除设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]
    *
    * @param ids 编号
    */
    void deleteDeviceCodecListByIds(List<Long> ids);

    /**
     * 获得设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]
     *
     * @param id 编号
     * @return 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]
     */
    DeviceCodecDO getDeviceCodec(Long id);

    /**
     * 获得设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]分页
     *
     * @param pageReqVO 分页查询
     * @return 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]分页
     */
    PageResult<DeviceCodecDO> getDeviceCodecPage(DeviceCodecPageReqVO pageReqVO);

}