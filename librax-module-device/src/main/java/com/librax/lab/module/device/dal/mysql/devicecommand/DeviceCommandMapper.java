package com.librax.lab.module.device.dal.mysql.devicecommand;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandPageReqVO;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface DeviceCommandMapper extends BaseMapperX<DeviceCommandDO> {

    default PageResult<DeviceCommandDO> selectPage(DeviceCommandPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DeviceCommandDO>()
                .eqIfPresent(DeviceCommandDO::getDeviceType, reqVO.getDeviceType())
                .eqIfPresent(DeviceCommandDO::getHttpPath, reqVO.getHttpPath())
                .betweenIfPresent(DeviceCommandDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DeviceCommandDO::getId));
    }

    default DeviceCommandDO selectByTypeAndCode(String deviceType, String commandCode) {
        return selectOne(DeviceCommandDO::getDeviceType, deviceType, DeviceCommandDO::getCommandCode, commandCode);
    }
}