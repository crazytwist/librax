package com.librax.lab.module.lab.service.materialinstance;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.materialinstance.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.materialinstance.MaterialInstanceMapper;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.service.slotinfo.SlotInfoService;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MaterialInstanceServiceImpl implements MaterialInstanceService {

    @Resource
    private MaterialInstanceMapper materialInstanceMapper;

    @Resource
    private SlotInfoService slotInfoService;

    @Override
    public Long createMaterialInstance(MaterialInstanceSaveReqVO createReqVO) {
        // 插入
        MaterialInstanceDO materialInstance = BeanUtils.toBean(createReqVO, MaterialInstanceDO.class);
        materialInstanceMapper.insert(materialInstance);

        // 返回
        return materialInstance.getId();
    }

    @Override
    public void updateMaterialInstance(MaterialInstanceSaveReqVO updateReqVO) {
        // 校验存在
        validateMaterialInstanceExists(updateReqVO.getId());
        // 更新
        MaterialInstanceDO updateObj = BeanUtils.toBean(updateReqVO, MaterialInstanceDO.class);
        materialInstanceMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaterialInstance(Long id) {
        // 校验存在
        validateMaterialInstanceExists(id);
        // 删除
        materialInstanceMapper.deleteById(id);
    }

    @Override
        public void deleteMaterialInstanceListByIds(List<Long> ids) {
        // 删除
        materialInstanceMapper.deleteByIds(ids);
        }


    private void validateMaterialInstanceExists(Long id) {
        if (materialInstanceMapper.selectById(id) == null) {
            throw exception(MATERIAL_INSTANCE_NOT_EXISTS);
        }
    }

    @Override
    public MaterialInstanceDO getMaterialInstance(Long id) {
        return materialInstanceMapper.selectById(id);
    }

    @Override
    public PageResult<MaterialInstanceDO> getMaterialInstancePage(MaterialInstancePageReqVO pageReqVO) {
        return materialInstanceMapper.selectPage(pageReqVO);
    }

    @Override
    public Map<String, MaterialInstanceDO> listBySlotIds(List<String> slotIds) {
        List<MaterialInstanceDO> list = materialInstanceMapper.selectBySlotIds(slotIds);
        Map<String, MaterialInstanceDO> result = new LinkedHashMap<>();
        for (MaterialInstanceDO item : list) {
            if (item.getSlotId() != null) {
                result.put(item.getSlotId(), item);
            }
        }
        return result;
    }

    @Override
    public List<String> batchLoadToSlot(List<com.librax.lab.module.lab.controller.admin.materialinstance.vo.MaterialInstanceBatchLoadReqVO.Item> items) {
        List<String> errors = new ArrayList<>();
        for (com.librax.lab.module.lab.controller.admin.materialinstance.vo.MaterialInstanceBatchLoadReqVO.Item item : items) {
            try {
                loadToSlot(item.getInstanceId(), item.getSlotId());
            } catch (Exception e) {
                errors.add(item.getInstanceId() + " → " + item.getSlotId() + "：" + e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public void batchUnloadFromSlot(List<String> instanceIds) {
        List<String> errors = new ArrayList<>();
        for (String instanceId : instanceIds) {
            try {
                unloadFromSlot(instanceId);
            } catch (Exception e) {
                errors.add(instanceId + "：" + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException("部分下架失败：" + String.join("；", errors));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void loadToSlot(String instanceId, String slotId) {
        // 1. 校验物料实例存在
        MaterialInstanceDO instance = materialInstanceMapper.selectByInstanceId(instanceId);
        if (instance == null) {
            throw exception(MATERIAL_INSTANCE_NOT_EXISTS);
        }
        // 2. 校验物料实例当前未绑定库位
        if (instance.getSlotId() != null) {
            throw exception(MATERIAL_INSTANCE_ALREADY_IN_SLOT);
        }
        // 3. 占用库位（内部校验库位存在/可用/未停用）
        slotInfoService.occupySlot(slotId, instanceId);
        // 4. 更新物料实例的库位和区域
        SlotInfoDO slot = slotInfoService.getSlotInfoBySlotId(slotId);
        materialInstanceMapper.update(null, new LambdaUpdateWrapper<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getInstanceId, instanceId)
                .set(MaterialInstanceDO::getSlotId, slotId)
                .set(MaterialInstanceDO::getZoneCode, slot.getZoneCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unloadFromSlot(String instanceId) {
        // 1. 校验物料实例存在
        MaterialInstanceDO instance = materialInstanceMapper.selectByInstanceId(instanceId);
        if (instance == null) {
            throw exception(MATERIAL_INSTANCE_NOT_EXISTS);
        }
        // 2. 校验物料实例当前在某个库位
        if (instance.getSlotId() == null) {
            throw exception(MATERIAL_INSTANCE_NOT_IN_SLOT);
        }
        // 3. 释放库位
        slotInfoService.releaseSlot(instance.getSlotId());
        // 4. 清除物料实例的库位和区域
        materialInstanceMapper.update(null, new LambdaUpdateWrapper<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getInstanceId, instanceId)
                .set(MaterialInstanceDO::getSlotId, null)
                .set(MaterialInstanceDO::getZoneCode, null));
    }

    @Override
    public MaterialInstanceDO getInstanceByInsId(String instanceId) {
        return materialInstanceMapper.selectByInstanceId(instanceId);
    }

    @Override
    public int moveTo(String instanceId, String slotId) {
        return moveTo(instanceId, slotId, "AVAILABLE");
    }

    @Override
    public int moveTo(String instanceId, String slotId, String targetStatus) {
        SlotInfoDO slot = slotInfoService.getSlotInfoBySlotId(slotId);
        if (slot == null) {
            throw new IllegalStateException("目标库位不存在: " + slotId);
        }
        return materialInstanceMapper.completeTransfer(instanceId, slotId, slot.getZoneCode(), targetStatus);
    }

}