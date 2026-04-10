package com.librax.lab.module.device.service.devicecodec;

import com.librax.lab.module.device.controller.admin.devicecodec.vo.DeviceCodecPageReqVO;
import com.librax.lab.module.device.controller.admin.devicecodec.vo.DeviceCodecSaveReqVO;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.devicecodec.vo.*;
import com.librax.lab.module.device.dal.dataobject.devicecodec.DeviceCodecDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.device.dal.mysql.devicecodec.DeviceCodecMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.module.device.enums.ErrorCodeConstants.*;

/**
 * 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class DeviceCodecServiceImpl implements DeviceCodecService {

    @Resource
    private DeviceCodecMapper deviceCodecMapper;

    @Override
    public Long createDeviceCodec(DeviceCodecSaveReqVO createReqVO) {
        // 插入
        DeviceCodecDO deviceCodec = BeanUtils.toBean(createReqVO, DeviceCodecDO.class);
        deviceCodecMapper.insert(deviceCodec);

        // 返回
        return deviceCodec.getId();
    }

    @Override
    public void updateDeviceCodec(DeviceCodecSaveReqVO updateReqVO) {
        // 校验存在
        validateDeviceCodecExists(updateReqVO.getId());
        // 更新
        DeviceCodecDO updateObj = BeanUtils.toBean(updateReqVO, DeviceCodecDO.class);
        deviceCodecMapper.updateById(updateObj);
    }

    @Override
    public void deleteDeviceCodec(Long id) {
        // 校验存在
        validateDeviceCodecExists(id);
        // 删除
        deviceCodecMapper.deleteById(id);
    }

    @Override
        public void deleteDeviceCodecListByIds(List<Long> ids) {
        // 删除
        deviceCodecMapper.deleteByIds(ids);
        }


    private void validateDeviceCodecExists(Long id) {
        if (deviceCodecMapper.selectById(id) == null) {
            throw exception(DEVICE_CODEC_NOT_EXISTS);
        }
    }

    @Override
    public DeviceCodecDO getDeviceCodec(Long id) {
        return deviceCodecMapper.selectById(id);
    }

    @Override
    public PageResult<DeviceCodecDO> getDeviceCodecPage(DeviceCodecPageReqVO pageReqVO) {
        return deviceCodecMapper.selectPage(pageReqVO);
    }

}