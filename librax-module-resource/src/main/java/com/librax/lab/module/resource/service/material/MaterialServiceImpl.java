package com.librax.lab.module.resource.service.material;

import cn.hutool.core.collection.CollUtil;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.resource.controller.admin.material.vo.MaterialPageReqVO;
import com.librax.lab.module.resource.controller.admin.material.vo.MaterialSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.material.MaterialDO;
import com.librax.lab.module.resource.dal.mysql.material.MaterialMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.MATERIAL_NOT_EXISTS;

/**
 * 物料基础信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MaterialServiceImpl implements MaterialService {

    @Resource
    private MaterialMapper materialMapper;

    @Override
    public Long createMaterial(MaterialSaveReqVO createReqVO) {
        // 插入
        MaterialDO material = BeanUtils.toBean(createReqVO, MaterialDO.class);
        materialMapper.insert(material);

        // 返回
        return material.getId();
    }

    @Override
    public void updateMaterial(MaterialSaveReqVO updateReqVO) {
        // 校验存在
        validateMaterialExists(updateReqVO.getId());
        // 更新
        MaterialDO updateObj = BeanUtils.toBean(updateReqVO, MaterialDO.class);
        materialMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaterial(Long id) {
        // 校验存在
        validateMaterialExists(id);
        // 删除
        materialMapper.deleteById(id);
    }

    @Override
        public void deleteMaterialListByIds(List<Long> ids) {
        // 删除
        materialMapper.deleteByIds(ids);
        }


    private void validateMaterialExists(Long id) {
        if (materialMapper.selectById(id) == null) {
            throw exception(MATERIAL_NOT_EXISTS);
        }
    }

    @Override
    public MaterialDO getMaterial(Long id) {
        return materialMapper.selectById(id);
    }

    @Override
    public PageResult<MaterialDO> getMaterialPage(MaterialPageReqVO pageReqVO) {
        return materialMapper.selectPage(pageReqVO);
    }

}