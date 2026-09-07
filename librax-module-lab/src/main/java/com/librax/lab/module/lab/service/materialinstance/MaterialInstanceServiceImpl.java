package com.librax.lab.module.lab.service.materialinstance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.exception.ServiceException;
import com.librax.lab.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.librax.lab.module.lab.dal.dataobject.materialdef.MaterialDefDO;
import com.librax.lab.module.lab.dal.mysql.materialdef.MaterialDefMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import com.librax.lab.module.lab.controller.admin.materialinstance.vo.*;
import com.librax.lab.module.lab.controller.app.materialinstance.vo.AppMaterialInstanceBatchUpdateRemainReqVO;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.materialinstance.MaterialInstanceMapper;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.service.slotinfo.SlotInfoService;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertSet;
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

    @Resource
    private MaterialDefMapper materialDefMapper;

    @Override
    public Long createMaterialInstance(MaterialInstanceSaveReqVO createReqVO) {
        // 实例保存数据校验，如果有物料，根据对应的内容类型，必须有对应的当前量值
        checkRequestParam(createReqVO);

        // 插入
        MaterialInstanceDO materialInstance = BeanUtils.toBean(createReqVO, MaterialInstanceDO.class);
        materialInstanceMapper.insert(materialInstance);

        // 返回
        return materialInstance.getId();
    }

    private void checkRequestParam(MaterialInstanceSaveReqVO createReqVO) {
        String materialCode = createReqVO.getMaterialCode();
        if (StrUtil.isNotBlank(materialCode)) {
            LambdaQueryWrapper<MaterialDefDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MaterialDefDO::getMaterialCode, materialCode);
            queryWrapper.eq(MaterialDefDO::getDeleted, false);
            MaterialDefDO materialDefDO = materialDefMapper.selectOne(queryWrapper);
            if (materialDefDO == null) {
                throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                        "物料定义不存在：" + materialCode);
            }
            String materialDefType = materialDefDO.getContentType();
            if ("SOLID".equals(materialDefType) && createReqVO.getCurrentCount() == null) {
                throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                        "物料定义类型为固体，当前量值不能为空");
            }
            if ("REAGENT".equals(materialDefType) && createReqVO.getCurrentVolUl() == null) {
                throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                        "物料定义类型为试剂，当前量值不能为空");
            }
        }
    }

    @Override
    public void updateMaterialInstance(MaterialInstanceSaveReqVO updateReqVO) {
        checkRequestParam(updateReqVO);
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
    public List<String> batchLoadToSlot(
            List<com.librax.lab.module.lab.controller.admin.materialinstance.vo.MaterialInstanceBatchLoadReqVO.Item> items) {
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
    public List<String> batchUpdateRemain(List<AppMaterialInstanceBatchUpdateRemainReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        // 1. 一次 IN 查询出所有存在的实例，避免逐条查询
        Set<String> existInstanceIds = convertSet(materialInstanceMapper.selectByInstanceIds(
                        convertSet(items, AppMaterialInstanceBatchUpdateRemainReqVO.Item::getInstanceId)),
                MaterialInstanceDO::getInstanceId);
        // 2. 过滤出存在的明细，收集不存在条目的错误信息
        List<AppMaterialInstanceBatchUpdateRemainReqVO.Item> validItems = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (AppMaterialInstanceBatchUpdateRemainReqVO.Item item : items) {
            if (existInstanceIds.contains(item.getInstanceId())) {
                validItems.add(item);
            } else {
                errors.add(item.getInstanceId() + "：物料实例不存在");
            }
        }
        // 3. 单条 SQL 批量更新剩余量
        if (CollUtil.isNotEmpty(validItems)) {
            materialInstanceMapper.updateRemainBatch(validItems);
        }
        return errors;
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
            // 校验 如果当前位置需要绑定的物料和已绑定的一直，则直接return
            if (instance.getSlotId().equals(slotId)) {
                return;
            }
            throw exception(MATERIAL_INSTANCE_ALREADY_IN_SLOT);
        }

        // 3. 占用库位（内部校验库位存在/可用/未停用）
        slotInfoService.occupySlot(slotId, instanceId);
        // 4. 更新物料实例的库位和区域
        SlotInfoDO slot = slotInfoService.getSlotInfoBySlotId(slotId);
        materialInstanceMapper.update(null,
                new LambdaUpdateWrapper<MaterialInstanceDO>().eq(MaterialInstanceDO::getInstanceId, instanceId)
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
        materialInstanceMapper.update(null,
                new LambdaUpdateWrapper<MaterialInstanceDO>().eq(MaterialInstanceDO::getInstanceId, instanceId)
                        .set(MaterialInstanceDO::getSlotId, null).set(MaterialInstanceDO::getZoneCode, null));
    }

    @Override
    public MaterialInstanceDO getInstanceByInsId(String instanceId) {
        return materialInstanceMapper.selectByInstanceId(instanceId);
    }

    @Override
    public void updateGloveboxRemark(String instId, String gloveboxTargetSlotId, Boolean hasCap) {
        // 1. 查询并校验物料实例存在
        MaterialInstanceDO instance = materialInstanceMapper.selectByInstanceId(instId);
        if (instance == null) {
            throw exception(MATERIAL_INSTANCE_NOT_EXISTS);
        }
        // 2. 解析原 remark：合法 JSON 对象则在其基础上补充，否则（空/非 JSON）重新组装
        JSONObject remarkObj = null;
        String remark = instance.getRemark();
        if (StrUtil.isNotBlank(remark)) {
            try {
                remarkObj = JSON.parseObject(remark);
            } catch (Exception ignore) {
                remarkObj = null; // 非 JSON 文本，忽略并按新 JSON 组装
            }
        }
        if (remarkObj == null) {
            remarkObj = new JSONObject();
        }
        // 3. 写入/移除合成站目标位与带盖信息：参数有值则写入（覆盖），为空则从 JSON 中移除对应配置
        if (StrUtil.isNotBlank(gloveboxTargetSlotId)) {
            remarkObj.put("gloveboxTargetSlotId", gloveboxTargetSlotId);
        } else {
            remarkObj.remove("gloveboxTargetSlotId");
        }
        if (hasCap != null) {
            remarkObj.put("hasCap", hasCap);
        } else {
            remarkObj.remove("hasCap");
        }
        // 4. 组装 remark 后以实体对象方式更新
        instance.setRemark(remarkObj.toJSONString());
        materialInstanceMapper.updateById(instance);
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