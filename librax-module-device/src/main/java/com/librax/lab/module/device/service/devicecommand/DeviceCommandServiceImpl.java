package com.librax.lab.module.device.service.devicecommand;

import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandPageReqVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandSaveReqVO;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.devicecommand.vo.*;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.device.dal.mysql.devicecommand.DeviceCommandMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.module.device.enums.ErrorCodeConstants.*;

/**
 * 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class DeviceCommandServiceImpl implements DeviceCommandService {

    @Resource
    private DeviceCommandMapper deviceCommandMapper;

    @Override
    public Long createDeviceCommand(DeviceCommandSaveReqVO createReqVO) {
        // 插入
        DeviceCommandDO deviceCommand = BeanUtils.toBean(createReqVO, DeviceCommandDO.class);
        deviceCommandMapper.insert(deviceCommand);

        // 返回
        return deviceCommand.getId();
    }

    @Override
    public void updateDeviceCommand(DeviceCommandSaveReqVO updateReqVO) {
        // 校验存在
        validateDeviceCommandExists(updateReqVO.getId());
        // 更新
        DeviceCommandDO updateObj = BeanUtils.toBean(updateReqVO, DeviceCommandDO.class);
        deviceCommandMapper.updateById(updateObj);
    }

    @Override
    public void deleteDeviceCommand(Long id) {
        // 校验存在
        validateDeviceCommandExists(id);
        // 删除
        deviceCommandMapper.deleteById(id);
    }

    @Override
        public void deleteDeviceCommandListByIds(List<Long> ids) {
        // 删除
        deviceCommandMapper.deleteByIds(ids);
        }


    private void validateDeviceCommandExists(Long id) {
        if (deviceCommandMapper.selectById(id) == null) {
            throw exception(DEVICE_COMMAND_NOT_EXISTS);
        }
    }

    @Override
    public DeviceCommandDO getDeviceCommand(Long id) {
        return deviceCommandMapper.selectById(id);
    }

    @Override
    public PageResult<DeviceCommandDO> getDeviceCommandPage(DeviceCommandPageReqVO pageReqVO) {
        return deviceCommandMapper.selectPage(pageReqVO);
    }

}