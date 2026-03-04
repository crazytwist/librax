package com.librax.lab.module.flow.service.definition;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.definition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.definition.FlowDefinitionDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 流程定义 Service 接口
 *
 * @author 芋道源码
 */
public interface DefinitionService {

    /**
     * 创建流程定义
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDefinition(@Valid DefinitionSaveReqVO createReqVO);

    /**
     * 更新流程定义
     *
     * @param updateReqVO 更新信息
     */
    void updateDefinition(@Valid DefinitionSaveReqVO updateReqVO);

    /**
     * 删除流程定义
     *
     * @param id 编号
     */
    void deleteDefinition(Long id);

    /**
    * 批量删除流程定义
    *
    * @param ids 编号
    */
    void deleteDefinitionListByIds(List<Long> ids);

    /**
     * 获得流程定义
     *
     * @param id 编号
     * @return 流程定义
     */
    FlowDefinitionDO getDefinition(Long id);

    /**
     * 获得流程定义分页
     *
     * @param pageReqVO 分页查询
     * @return 流程定义分页
     */
    PageResult<FlowDefinitionDO> getDefinitionPage(DefinitionPageReqVO pageReqVO);

}