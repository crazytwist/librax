package com.librax.lab.module.resource.service.resourceconfig;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.resource.controller.admin.resourceconfig.vo.*;
import com.librax.lab.module.resource.dal.dataobject.resourceconfig.ResourceConfigDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 资源配置表,运行时锁状态见Redis Service 接口
 *
 * @author 一南
 */
public interface ResourceConfigService {

    /**
     * 创建资源配置表,运行时锁状态见Redis
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createConfig(@Valid ResourceConfigSaveReqVO createReqVO);

    /**
     * 更新资源配置表,运行时锁状态见Redis
     *
     * @param updateReqVO 更新信息
     */
    void updateConfig(@Valid ResourceConfigSaveReqVO updateReqVO);

    /**
     * 删除资源配置表,运行时锁状态见Redis
     *
     * @param id 编号
     */
    void deleteConfig(Long id);

    /**
    * 批量删除资源配置表,运行时锁状态见Redis
    *
    * @param ids 编号
    */
    void deleteConfigListByIds(List<Long> ids);

    /**
     * 获得资源配置表,运行时锁状态见Redis
     *
     * @param id 编号
     * @return 资源配置表,运行时锁状态见Redis
     */
    ResourceConfigDO getConfig(Long id);

    /**
     * 获得资源配置表,运行时锁状态见Redis分页
     *
     * @param pageReqVO 分页查询
     * @return 资源配置表,运行时锁状态见Redis分页
     */
    PageResult<ResourceConfigDO> getConfigPage(ResourceConfigPageReqVO pageReqVO);

}