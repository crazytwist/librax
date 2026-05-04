package com.librax.lab.module.lab.service.materialinstance;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.materialinstance.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理 Service 接口
 *
 * @author 芋道源码
 */
public interface MaterialInstanceService {

    /**
     * 创建物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMaterialInstance(@Valid MaterialInstanceSaveReqVO createReqVO);

    /**
     * 更新物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理
     *
     * @param updateReqVO 更新信息
     */
    void updateMaterialInstance(@Valid MaterialInstanceSaveReqVO updateReqVO);

    /**
     * 删除物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理
     *
     * @param id 编号
     */
    void deleteMaterialInstance(Long id);

    /**
    * 批量删除物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理
    *
    * @param ids 编号
    */
    void deleteMaterialInstanceListByIds(List<Long> ids);

    /**
     * 获得物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理
     *
     * @param id 编号
     * @return 物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理
     */
    MaterialInstanceDO getMaterialInstance(Long id);

    /**
     * 获得物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理分页
     *
     * @param pageReqVO 分页查询
     * @return 物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理分页
     */
    PageResult<MaterialInstanceDO> getMaterialInstancePage(MaterialInstancePageReqVO pageReqVO);

}