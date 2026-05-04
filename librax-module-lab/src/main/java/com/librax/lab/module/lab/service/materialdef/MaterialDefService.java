package com.librax.lab.module.lab.service.materialdef;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.materialdef.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialdef.MaterialDefDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理 Service 接口
 *
 * @author 芋道源码
 */
public interface MaterialDefService {

    /**
     * 创建内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMaterialDef(@Valid MaterialDefSaveReqVO createReqVO);

    /**
     * 更新内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理
     *
     * @param updateReqVO 更新信息
     */
    void updateMaterialDef(@Valid MaterialDefSaveReqVO updateReqVO);

    /**
     * 删除内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理
     *
     * @param id 编号
     */
    void deleteMaterialDef(Long id);

    /**
    * 批量删除内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理
    *
    * @param ids 编号
    */
    void deleteMaterialDefListByIds(List<Long> ids);

    /**
     * 获得内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理
     *
     * @param id 编号
     * @return 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理
     */
    MaterialDefDO getMaterialDef(Long id);

    /**
     * 获得内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理分页
     *
     * @param pageReqVO 分页查询
     * @return 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理分页
     */
    PageResult<MaterialDefDO> getMaterialDefPage(MaterialDefPageReqVO pageReqVO);

}