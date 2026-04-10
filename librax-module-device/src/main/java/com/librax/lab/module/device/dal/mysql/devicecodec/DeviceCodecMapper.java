package com.librax.lab.module.device.dal.mysql.devicecodec;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.device.controller.admin.devicecodec.vo.DeviceCodecPageReqVO;
import com.librax.lab.module.device.dal.dataobject.devicecodec.DeviceCodecDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface DeviceCodecMapper extends BaseMapperX<DeviceCodecDO> {

    default PageResult<DeviceCodecDO> selectPage(DeviceCodecPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DeviceCodecDO>()
                .likeIfPresent(DeviceCodecDO::getCodecName, reqVO.getCodecName())
                .eqIfPresent(DeviceCodecDO::getParseType, reqVO.getParseType())
                .eqIfPresent(DeviceCodecDO::getFieldMapping, reqVO.getFieldMapping())
                .eqIfPresent(DeviceCodecDO::getHexRules, reqVO.getHexRules())
                .eqIfPresent(DeviceCodecDO::getRegexRules, reqVO.getRegexRules())
                .eqIfPresent(DeviceCodecDO::getScriptEngine, reqVO.getScriptEngine())
                .eqIfPresent(DeviceCodecDO::getScriptContent, reqVO.getScriptContent())
                .eqIfPresent(DeviceCodecDO::getUnitConversions, reqVO.getUnitConversions())
                .eqIfPresent(DeviceCodecDO::getValidRange, reqVO.getValidRange())
                .eqIfPresent(DeviceCodecDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(DeviceCodecDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DeviceCodecDO::getId));
    }

}