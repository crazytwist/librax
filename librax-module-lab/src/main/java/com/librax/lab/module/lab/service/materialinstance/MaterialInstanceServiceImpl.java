package com.librax.lab.module.lab.service.materialinstance;

import cn.hutool.core.collection.CollUtil;
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

}