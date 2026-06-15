package com.librax.lab.module.resource.service.slotinfo;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.resource.controller.admin.slotinfo.vo.*;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理 Service 接口
 *
 * @author 一南
 */
public interface SlotInfoService {

    /**
     * 创建库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSlotInfo(@Valid SlotInfoSaveReqVO createReqVO);

    /**
     * 更新库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理
     *
     * @param updateReqVO 更新信息
     */
    void updateSlotInfo(@Valid SlotInfoSaveReqVO updateReqVO);

    /**
     * 删除库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理
     *
     * @param id 编号
     */
    void deleteSlotInfo(Long id);

    /**
    * 批量删除库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理
    *
    * @param ids 编号
    */
    void deleteSlotInfoListByIds(List<Long> ids);

    /**
     * 获得库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理
     *
     * @param id 编号
     * @return 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理
     */
    SlotInfoDO getSlotInfo(Long id);

    /**
     * 获得库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理分页
     *
     * @param pageReqVO 分页查询
     * @return 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理分页
     */
    PageResult<SlotInfoDO> getSlotInfoPage(SlotInfoPageReqVO pageReqVO);

    /**
     * 按 slotId 查询库位
     */
    SlotInfoDO getSlotInfoBySlotId(String slotId);

    /**
     * 物料上架：占用库位，校验库位可用性，status → OCCUPIED
     *
     * @param slotId     库位编码
     * @param instanceId 物料实例ID
     */
    void occupySlot(String slotId, String instanceId);

    /**
     * 物料下架：释放库位，currentCount -1，降到 0 时 status → EMPTY
     *
     * @param slotId 库位编码
     */
    void releaseSlot(String slotId);

}