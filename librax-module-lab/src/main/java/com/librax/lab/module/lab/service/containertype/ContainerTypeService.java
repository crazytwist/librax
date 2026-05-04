package com.librax.lab.module.lab.service.containertype;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.containertype.vo.*;
import com.librax.lab.module.lab.dal.dataobject.containertype.ContainerTypeDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理 Service 接口
 *
 * @author 芋道源码
 */
public interface ContainerTypeService {

    /**
     * 创建容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createContainerType(@Valid ContainerTypeSaveReqVO createReqVO);

    /**
     * 更新容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理
     *
     * @param updateReqVO 更新信息
     */
    void updateContainerType(@Valid ContainerTypeSaveReqVO updateReqVO);

    /**
     * 删除容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理
     *
     * @param id 编号
     */
    void deleteContainerType(Long id);

    /**
    * 批量删除容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理
    *
    * @param ids 编号
    */
    void deleteContainerTypeListByIds(List<Long> ids);

    /**
     * 获得容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理
     *
     * @param id 编号
     * @return 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理
     */
    ContainerTypeDO getContainerType(Long id);

    /**
     * 获得容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理分页
     *
     * @param pageReqVO 分页查询
     * @return 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理分页
     */
    PageResult<ContainerTypeDO> getContainerTypePage(ContainerTypePageReqVO pageReqVO);

}