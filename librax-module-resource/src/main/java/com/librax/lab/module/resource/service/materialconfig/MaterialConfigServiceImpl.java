package com.librax.lab.module.resource.service.materialconfig;

import cn.hutool.core.collection.CollUtil;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.resource.controller.admin.materialconfig.vo.MaterialConfigPageReqVO;
import com.librax.lab.module.resource.controller.admin.materialconfig.vo.MaterialConfigSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.materialconfig.MaterialConfigDO;
import com.librax.lab.module.resource.dal.mysql.materialconfig.MaterialConfigMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.MATERIAL_CONFIG_NOT_EXISTS;

/**
 * 物料配置 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MaterialConfigServiceImpl implements MaterialConfigService {

    @Resource
    private MaterialConfigMapper materialConfigMapper;

    @Override
    public Long createMaterialConfig(MaterialConfigSaveReqVO createReqVO) {
        // 插入
        MaterialConfigDO materialConfig = BeanUtils.toBean(createReqVO, MaterialConfigDO.class);
        materialConfigMapper.insert(materialConfig);

        // 返回
        return materialConfig.getId();
    }

    @Override
    public void updateMaterialConfig(MaterialConfigSaveReqVO updateReqVO) {
        // 校验存在
        validateMaterialConfigExists(updateReqVO.getId());
        // 更新
        MaterialConfigDO updateObj = BeanUtils.toBean(updateReqVO, MaterialConfigDO.class);
        materialConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaterialConfig(Long id) {
        // 校验存在
        validateMaterialConfigExists(id);
        // 删除
        materialConfigMapper.deleteById(id);
    }

    @Override
        public void deleteMaterialConfigListByIds(List<Long> ids) {
        // 删除
        materialConfigMapper.deleteByIds(ids);
        }


    private void validateMaterialConfigExists(Long id) {
        if (materialConfigMapper.selectById(id) == null) {
            throw exception(MATERIAL_CONFIG_NOT_EXISTS);
        }
    }

    @Override
    public MaterialConfigDO getMaterialConfig(Long id) {
        return materialConfigMapper.selectById(id);
    }

    @Override
    public PageResult<MaterialConfigDO> getMaterialConfigPage(MaterialConfigPageReqVO pageReqVO) {
        return materialConfigMapper.selectPage(pageReqVO);
    }

}