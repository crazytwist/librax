package com.librax.lab.module.lab.service.materialconsumption;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.materialconsumption.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialconsumption.MaterialConsumptionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.materialconsumption.MaterialConsumptionMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MaterialConsumptionServiceImpl implements MaterialConsumptionService {

    @Resource
    private MaterialConsumptionMapper materialConsumptionMapper;

    @Override
    public Long createMaterialConsumption(MaterialConsumptionSaveReqVO createReqVO) {
        // 插入
        MaterialConsumptionDO materialConsumption = BeanUtils.toBean(createReqVO, MaterialConsumptionDO.class);
        materialConsumptionMapper.insert(materialConsumption);

        // 返回
        return materialConsumption.getId();
    }

    @Override
    public void updateMaterialConsumption(MaterialConsumptionSaveReqVO updateReqVO) {
        // 校验存在
        validateMaterialConsumptionExists(updateReqVO.getId());
        // 更新
        MaterialConsumptionDO updateObj = BeanUtils.toBean(updateReqVO, MaterialConsumptionDO.class);
        materialConsumptionMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaterialConsumption(Long id) {
        // 校验存在
        validateMaterialConsumptionExists(id);
        // 删除
        materialConsumptionMapper.deleteById(id);
    }

    @Override
        public void deleteMaterialConsumptionListByIds(List<Long> ids) {
        // 删除
        materialConsumptionMapper.deleteByIds(ids);
        }


    private void validateMaterialConsumptionExists(Long id) {
        if (materialConsumptionMapper.selectById(id) == null) {
            throw exception(MATERIAL_CONSUMPTION_NOT_EXISTS);
        }
    }

    @Override
    public MaterialConsumptionDO getMaterialConsumption(Long id) {
        return materialConsumptionMapper.selectById(id);
    }

    @Override
    public PageResult<MaterialConsumptionDO> getMaterialConsumptionPage(MaterialConsumptionPageReqVO pageReqVO) {
        return materialConsumptionMapper.selectPage(pageReqVO);
    }

}