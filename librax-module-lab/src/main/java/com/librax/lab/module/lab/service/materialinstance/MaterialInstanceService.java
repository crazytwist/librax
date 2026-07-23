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
     * 根据库位ID列表批量查询已绑定的物料实例
     *
     * @param slotIds 库位ID列表
     * @return slotId → MaterialInstanceDO，未绑定的库位不出现在 Map 中
     */
    Map<String, MaterialInstanceDO> listBySlotIds(List<String> slotIds);

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


    /**
     * 根据 instanceId 查询物料实例
     *
     * @param instanceId 物料实例ID
     * @return 物料实例
     */
    MaterialInstanceDO getInstanceByInsId(String instanceId);

    /**
     * 物料位置提交：将物料移动到新库位，zoneCode 强制从 slotInfo 读取，状态恢复为 AVAILABLE。
     *
     * <p>适用场景：AGV 补料流程完成后，提交物料的最终位置（送达机台站位，可被取用）。
     *
     * @param instanceId 物料实例ID
     * @param slotId     目标库位ID（必须存在且已启用）
     * @return 更新行数，0 表示物料状态已变更（被并发操作），需上层处理
     */
    int moveTo(String instanceId, String slotId);

    /**
     * 物料位置提交（通用）：将物料移动到新库位，状态恢复为搬运前的原始状态。
     *
     * <p>搬运不改变物料的语义状态：原始状态是什么，到达新位置后就恢复为什么。
     * 例如：补料时 AVAILABLE→AVAILABLE，下料时 USED→USED。
     *
     * @param instanceId   物料实例ID
     * @param slotId       目标库位ID（必须存在且已启用）
     * @param targetStatus 目标状态（搬运前的原始状态）
     * @return 更新行数，0 表示物料状态已变更（被并发操作），需上层处理
     */
    int moveTo(String instanceId, String slotId, String targetStatus);

}