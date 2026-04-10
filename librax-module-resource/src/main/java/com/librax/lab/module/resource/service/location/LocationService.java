package com.librax.lab.module.resource.service.location;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.module.resource.controller.admin.location.vo.LocationPageReqVO;
import com.librax.lab.module.resource.controller.admin.location.vo.LocationSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.location.LocationDO;
import jakarta.validation.*;

/**
 * 区位信息 Service 接口
 *
 * @author 一南
 */
public interface LocationService {

    /**
     * 创建区位信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLocation(@Valid LocationSaveReqVO createReqVO);

    /**
     * 更新区位信息
     *
     * @param updateReqVO 更新信息
     */
    void updateLocation(@Valid LocationSaveReqVO updateReqVO);

    /**
     * 删除区位信息
     *
     * @param id 编号
     */
    void deleteLocation(Long id);

    /**
    * 批量删除区位信息
    *
    * @param ids 编号
    */
    void deleteLocationListByIds(List<Long> ids);

    /**
     * 获得区位信息
     *
     * @param id 编号
     * @return 区位信息
     */
    LocationDO getLocation(Long id);

    /**
     * 获得区位信息分页
     *
     * @param pageReqVO 分页查询
     * @return 区位信息分页
     */
    PageResult<LocationDO> getLocationPage(LocationPageReqVO pageReqVO);

}