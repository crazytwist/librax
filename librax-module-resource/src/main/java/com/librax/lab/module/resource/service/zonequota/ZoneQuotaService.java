package com.librax.lab.module.resource.service.zonequota;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.resource.controller.admin.zonequota.vo.*;
import com.librax.lab.module.resource.dal.dataobject.zonequota.ZoneQuotaDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 区域对共享资源的配额 Service 接口
 *
 * @author 芋道源码
 */
public interface ZoneQuotaService {

    /**
     * 创建区域对共享资源的配额
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createZoneQuota(@Valid ZoneQuotaSaveReqVO createReqVO);

    /**
     * 更新区域对共享资源的配额
     *
     * @param updateReqVO 更新信息
     */
    void updateZoneQuota(@Valid ZoneQuotaSaveReqVO updateReqVO);

    /**
     * 删除区域对共享资源的配额
     *
     * @param id 编号
     */
    void deleteZoneQuota(Long id);

    /**
    * 批量删除区域对共享资源的配额
    *
    * @param ids 编号
    */
    void deleteZoneQuotaListByIds(List<Long> ids);

    /**
     * 获得区域对共享资源的配额
     *
     * @param id 编号
     * @return 区域对共享资源的配额
     */
    ZoneQuotaDO getZoneQuota(Long id);

    /**
     * 获得区域对共享资源的配额分页
     *
     * @param pageReqVO 分页查询
     * @return 区域对共享资源的配额分页
     */
    PageResult<ZoneQuotaDO> getZoneQuotaPage(ZoneQuotaPageReqVO pageReqVO);

}