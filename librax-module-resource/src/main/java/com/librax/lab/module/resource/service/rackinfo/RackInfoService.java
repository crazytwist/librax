package com.librax.lab.module.resource.service.rackinfo;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.resource.controller.admin.rackinfo.vo.*;
import com.librax.lab.module.resource.dal.dataobject.rackinfo.RackInfoDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 货架/台面定义，库位的上级容器，归 resource 模块管理 Service 接口
 *
 * @author 一南
 */
public interface RackInfoService {

    /**
     * 创建货架/台面定义，库位的上级容器，归 resource 模块管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createRackInfo(@Valid RackInfoSaveReqVO createReqVO);

    /**
     * 更新货架/台面定义，库位的上级容器，归 resource 模块管理
     *
     * @param updateReqVO 更新信息
     */
    void updateRackInfo(@Valid RackInfoSaveReqVO updateReqVO);

    /**
     * 删除货架/台面定义，库位的上级容器，归 resource 模块管理
     *
     * @param id 编号
     */
    void deleteRackInfo(Long id);

    /**
    * 批量删除货架/台面定义，库位的上级容器，归 resource 模块管理
    *
    * @param ids 编号
    */
    void deleteRackInfoListByIds(List<Long> ids);

    /**
     * 获得货架/台面定义，库位的上级容器，归 resource 模块管理
     *
     * @param id 编号
     * @return 货架/台面定义，库位的上级容器，归 resource 模块管理
     */
    RackInfoDO getRackInfo(Long id);

    /**
     * 获得货架/台面定义，库位的上级容器，归 resource 模块管理分页
     *
     * @param pageReqVO 分页查询
     * @return 货架/台面定义，库位的上级容器，归 resource 模块管理分页
     */
    PageResult<RackInfoDO> getRackInfoPage(RackInfoPageReqVO pageReqVO);

}