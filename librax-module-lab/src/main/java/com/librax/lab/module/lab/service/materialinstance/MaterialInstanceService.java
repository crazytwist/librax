package com.librax.lab.module.lab.service.materialinstance;

import java.util.*;
import java.util.List;
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

    /**
     * 批量上架：将多个物料实例分别绑定到指定库位，逐条执行，失败条目收集后统一抛出
     *
     * @param items instanceId → slotId 的映射列表
     * @return 失败条目信息（全部成功时为空列表）
     */
    List<String> batchLoadToSlot(List<com.librax.lab.module.lab.controller.admin.materialinstance.vo.MaterialInstanceBatchLoadReqVO.Item> items);

    /**
     * 批量下架：将多个物料实例从当前库位移除
     *
     * @param instanceIds 物料实例ID列表
     */
    void batchUnloadFromSlot(List<String> instanceIds);

    /**
     * 上架：将物料实例绑定到指定库位
     * <p>
     * 前置校验：物料实例存在、未在任何库位中；库位存在、非 OCCUPIED、非 DISABLED
     *
     * @param instanceId 物料实例ID（业务ID）
     * @param slotId     目标库位编码
     */
    void loadToSlot(String instanceId, String slotId);

    /**
     * 下架：将物料实例从当前库位移除
     * <p>
     * 前置校验：物料实例存在，且当前有绑定库位
     *
     * @param instanceId 物料实例ID（业务ID）
     */
    void unloadFromSlot(String instanceId);

}