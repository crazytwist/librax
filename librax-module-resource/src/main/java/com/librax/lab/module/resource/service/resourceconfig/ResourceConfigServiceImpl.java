package com.librax.lab.module.resource.service.resourceconfig;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.resource.controller.admin.resourceconfig.vo.*;
import com.librax.lab.module.resource.dal.dataobject.resourceconfig.ResourceConfigDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.resource.dal.mysql.resourceconfig.ResourceConfigMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.*;

/**
 * 资源配置表,运行时锁状态见Redis Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ResourceConfigServiceImpl implements ResourceConfigService {

    @Resource
    private ResourceConfigMapper resourceConfigMapper;

    @Override
    public Long createConfig(ResourceConfigSaveReqVO createReqVO) {
        // 插入
        ResourceConfigDO config = BeanUtils.toBean(createReqVO, ResourceConfigDO.class);
        resourceConfigMapper.insert(config);

        // 返回
        return config.getId();
    }

    @Override
    public void updateConfig(ResourceConfigSaveReqVO updateReqVO) {
        // 校验存在
        validateConfigExists(updateReqVO.getId());
        // 更新
        ResourceConfigDO updateObj = BeanUtils.toBean(updateReqVO, ResourceConfigDO.class);
        resourceConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteConfig(Long id) {
        // 校验存在
        validateConfigExists(id);
        // 删除
        resourceConfigMapper.deleteById(id);
    }

    @Override
        public void deleteConfigListByIds(List<Long> ids) {
        // 删除
        resourceConfigMapper.deleteByIds(ids);
        }


    private void validateConfigExists(Long id) {
        if (resourceConfigMapper.selectById(id) == null) {
            throw exception(CONFIG_NOT_EXISTS);
        }
    }

    @Override
    public ResourceConfigDO getConfig(Long id) {
        return resourceConfigMapper.selectById(id);
    }

    @Override
    public PageResult<ResourceConfigDO> getConfigPage(ResourceConfigPageReqVO pageReqVO) {
        return resourceConfigMapper.selectPage(pageReqVO);
    }

}