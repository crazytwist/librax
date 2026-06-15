package com.librax.lab.module.resource.service.slotinfo;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.resource.controller.admin.slotinfo.vo.*;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.resource.dal.mysql.slotinfo.SlotInfoMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.*;

/**
 * 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理 Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class SlotInfoServiceImpl implements SlotInfoService {

    @Resource
    private SlotInfoMapper slotInfoMapper;

    @Override
    public Long createSlotInfo(SlotInfoSaveReqVO createReqVO) {
        // 插入
        SlotInfoDO slotInfo = BeanUtils.toBean(createReqVO, SlotInfoDO.class);
        slotInfoMapper.insert(slotInfo);

        // 返回
        return slotInfo.getId();
    }

    @Override
    public void updateSlotInfo(SlotInfoSaveReqVO updateReqVO) {
        // 校验存在
        validateSlotInfoExists(updateReqVO.getId());
        // 更新
        SlotInfoDO updateObj = BeanUtils.toBean(updateReqVO, SlotInfoDO.class);
        slotInfoMapper.updateById(updateObj);
    }

    @Override
    public void deleteSlotInfo(Long id) {
        // 校验存在
        validateSlotInfoExists(id);
        // 删除
        slotInfoMapper.deleteById(id);
    }

    @Override
        public void deleteSlotInfoListByIds(List<Long> ids) {
        // 删除
        slotInfoMapper.deleteByIds(ids);
        }


    private void validateSlotInfoExists(Long id) {
        if (slotInfoMapper.selectById(id) == null) {
            throw exception(SLOT_INFO_NOT_EXISTS);
        }
    }

    @Override
    public SlotInfoDO getSlotInfo(Long id) {
        return slotInfoMapper.selectById(id);
    }

    @Override
    public PageResult<SlotInfoDO> getSlotInfoPage(SlotInfoPageReqVO pageReqVO) {
        return slotInfoMapper.selectPage(pageReqVO);
    }

    @Override
    public SlotInfoDO getSlotInfoBySlotId(String slotId) {
        return slotInfoMapper.selectBySlotId(slotId);
    }

    @Override
    public void occupySlot(String slotId, String instanceId) {
        SlotInfoDO slot = slotInfoMapper.selectBySlotId(slotId);
        if (slot == null) {
            throw exception(SLOT_INFO_NOT_EXISTS);
        }
        if ("DISABLED".equals(slot.getStatus())) {
            throw exception(SLOT_DISABLED);
        }
        if ("OCCUPIED".equals(slot.getStatus())) {
            throw exception(SLOT_ALREADY_OCCUPIED);
        }
        int capacity = slot.getCapacity() != null ? slot.getCapacity() : 1;
        slotInfoMapper.occupy(slotId, instanceId, capacity);
    }

    @Override
    public void releaseSlot(String slotId) {
        slotInfoMapper.release(slotId);
    }

}