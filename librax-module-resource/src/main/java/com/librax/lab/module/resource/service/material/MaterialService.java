package com.librax.lab.module.resource.service.material;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.module.resource.controller.admin.material.vo.MaterialPageReqVO;
import com.librax.lab.module.resource.controller.admin.material.vo.MaterialSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.material.MaterialDO;
import jakarta.validation.*;

/**
 * 物料基础信息 Service 接口
 *
 * @author 一南
 */
public interface MaterialService {

    /**
     * 创建物料基础信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMaterial(@Valid MaterialSaveReqVO createReqVO);

    /**
     * 更新物料基础信息
     *
     * @param updateReqVO 更新信息
     */
    void updateMaterial(@Valid MaterialSaveReqVO updateReqVO);

    /**
     * 删除物料基础信息
     *
     * @param id 编号
     */
    void deleteMaterial(Long id);

    /**
    * 批量删除物料基础信息
    *
    * @param ids 编号
    */
    void deleteMaterialListByIds(List<Long> ids);

    /**
     * 获得物料基础信息
     *
     * @param id 编号
     * @return 物料基础信息
     */
    MaterialDO getMaterial(Long id);

    /**
     * 获得物料基础信息分页
     *
     * @param pageReqVO 分页查询
     * @return 物料基础信息分页
     */
    PageResult<MaterialDO> getMaterialPage(MaterialPageReqVO pageReqVO);

}