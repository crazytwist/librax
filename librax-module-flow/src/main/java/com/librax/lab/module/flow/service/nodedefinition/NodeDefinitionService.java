package com.librax.lab.module.flow.service.nodedefinition;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.nodedefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.nodedefinition.NodeDefinitionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 流程节点定义 Service 接口
 *
 * @author 芋道源码
 */
public interface NodeDefinitionService {

    /**
     * 创建流程节点定义
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createNodeDefinition(@Valid NodeDefinitionSaveReqVO createReqVO);

    /**
     * 更新流程节点定义
     *
     * @param updateReqVO 更新信息
     */
    void updateNodeDefinition(@Valid NodeDefinitionSaveReqVO updateReqVO);

    /**
     * 删除流程节点定义
     *
     * @param id 编号
     */
    void deleteNodeDefinition(Long id);

    /**
    * 批量删除流程节点定义
    *
    * @param ids 编号
    */
    void deleteNodeDefinitionListByIds(List<Long> ids);

    /**
     * 获得流程节点定义
     *
     * @param id 编号
     * @return 流程节点定义
     */
    NodeDefinitionDO getNodeDefinition(Long id);

    /**
     * 获得流程节点定义分页
     *
     * @param pageReqVO 分页查询
     * @return 流程节点定义分页
     */
    PageResult<NodeDefinitionDO> getNodeDefinitionPage(NodeDefinitionPageReqVO pageReqVO);

}