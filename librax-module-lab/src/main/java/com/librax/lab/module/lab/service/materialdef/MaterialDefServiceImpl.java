package com.librax.lab.module.lab.service.materialdef;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.materialdef.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialdef.MaterialDefDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.materialdef.MaterialDefMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MaterialDefServiceImpl implements MaterialDefService {

    @Resource
    private MaterialDefMapper materialDefMapper;

    @Override
    public Long createMaterialDef(MaterialDefSaveReqVO createReqVO) {
        // 插入
        MaterialDefDO materialDef = BeanUtils.toBean(createReqVO, MaterialDefDO.class);
        materialDefMapper.insert(materialDef);

        // 返回
        return materialDef.getId();
    }

    @Override
    public void updateMaterialDef(MaterialDefSaveReqVO updateReqVO) {
        // 校验存在
        validateMaterialDefExists(updateReqVO.getId());
        // 更新
        MaterialDefDO updateObj = BeanUtils.toBean(updateReqVO, MaterialDefDO.class);
        materialDefMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaterialDef(Long id) {
        // 校验存在
        validateMaterialDefExists(id);
        // 删除
        materialDefMapper.deleteById(id);
    }

    @Override
        public void deleteMaterialDefListByIds(List<Long> ids) {
        // 删除
        materialDefMapper.deleteByIds(ids);
        }


    private void validateMaterialDefExists(Long id) {
        if (materialDefMapper.selectById(id) == null) {
            throw exception(MATERIAL_DEF_NOT_EXISTS);
        }
    }

    @Override
    public MaterialDefDO getMaterialDef(Long id) {
        return materialDefMapper.selectById(id);
    }

    @Override
    public PageResult<MaterialDefDO> getMaterialDefPage(MaterialDefPageReqVO pageReqVO) {
        return materialDefMapper.selectPage(pageReqVO);
    }

}