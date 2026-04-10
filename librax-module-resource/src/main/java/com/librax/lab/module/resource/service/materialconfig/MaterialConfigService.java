package com.librax.lab.module.resource.service.materialconfig;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.module.resource.controller.admin.materialconfig.vo.MaterialConfigPageReqVO;
import com.librax.lab.module.resource.controller.admin.materialconfig.vo.MaterialConfigSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.materialconfig.MaterialConfigDO;
import jakarta.validation.*;

/**
 * 物料配置 Service 接口
 *
 * @author 一南
 */
public interface MaterialConfigService {

    /**
     * 创建物料配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMaterialConfig(@Valid MaterialConfigSaveReqVO createReqVO);

    /**
     * 更新物料配置
     *
     * @param updateReqVO 更新信息
     */
    void updateMaterialConfig(@Valid MaterialConfigSaveReqVO updateReqVO);

    /**
     * 删除物料配置
     *
     * @param id 编号
     */
    void deleteMaterialConfig(Long id);

    /**
    * 批量删除物料配置
    *
    * @param ids 编号
    */
    void deleteMaterialConfigListByIds(List<Long> ids);

    /**
     * 获得物料配置
     *
     * @param id 编号
     * @return 物料配置
     */
    MaterialConfigDO getMaterialConfig(Long id);

    /**
     * 获得物料配置分页
     *
     * @param pageReqVO 分页查询
     * @return 物料配置分页
     */
    PageResult<MaterialConfigDO> getMaterialConfigPage(MaterialConfigPageReqVO pageReqVO);

}