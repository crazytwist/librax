package com.librax.lab.module.flow.service.pipelinestep;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.pipelinestep.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinestep.PipelineStepDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_] Service 接口
 *
 * @author 一南
 */
public interface PipelineStepService {

    /**
     * 创建流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPipelineStep(@Valid PipelineStepSaveReqVO createReqVO);

    /**
     * 更新流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]
     *
     * @param updateReqVO 更新信息
     */
    void updatePipelineStep(@Valid PipelineStepSaveReqVO updateReqVO);

    /**
     * 删除流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]
     *
     * @param id 编号
     */
    void deletePipelineStep(Long id);

    /**
    * 批量删除流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]
    *
    * @param ids 编号
    */
    void deletePipelineStepListByIds(List<Long> ids);

    /**
     * 获得流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]
     *
     * @param id 编号
     * @return 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]
     */
    PipelineStepDO getPipelineStep(Long id);

    /**
     * 获得流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]分页
     *
     * @param pageReqVO 分页查询
     * @return 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]分页
     */
    PageResult<PipelineStepDO> getPipelineStepPage(PipelineStepPageReqVO pageReqVO);

}