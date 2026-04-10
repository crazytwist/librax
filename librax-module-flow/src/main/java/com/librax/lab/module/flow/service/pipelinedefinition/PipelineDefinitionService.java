package com.librax.lab.module.flow.service.pipelinedefinition;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.pipelinedefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinedefinition.PipelineDefinitionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_] Service 接口
 *
 * @author 一南
 */
public interface PipelineDefinitionService {

    /**
     * 创建流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPipelineDefinition(@Valid PipelineDefinitionSaveReqVO createReqVO);

    /**
     * 更新流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]
     *
     * @param updateReqVO 更新信息
     */
    void updatePipelineDefinition(@Valid PipelineDefinitionSaveReqVO updateReqVO);

    /**
     * 删除流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]
     *
     * @param id 编号
     */
    void deletePipelineDefinition(Long id);

    /**
    * 批量删除流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]
    *
    * @param ids 编号
    */
    void deletePipelineDefinitionListByIds(List<Long> ids);

    /**
     * 获得流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]
     *
     * @param id 编号
     * @return 流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]
     */
    PipelineDefinitionDO getPipelineDefinition(Long id);

    /**
     * 获得流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]分页
     *
     * @param pageReqVO 分页查询
     * @return 流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]分页
     */
    PageResult<PipelineDefinitionDO> getPipelineDefinitionPage(PipelineDefinitionPageReqVO pageReqVO);

}