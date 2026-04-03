package com.librax.lab.module.flow.service.pipelinetrigger;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.pipelinetrigger.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinetrigger.PipelineTriggerDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 流程触发配置表，管理定时和事件触发规则 [pd_] Service 接口
 *
 * @author 一南
 */
public interface PipelineTriggerService {

    /**
     * 创建流程触发配置表，管理定时和事件触发规则 [pd_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPipelineTrigger(@Valid PipelineTriggerSaveReqVO createReqVO);

    /**
     * 更新流程触发配置表，管理定时和事件触发规则 [pd_]
     *
     * @param updateReqVO 更新信息
     */
    void updatePipelineTrigger(@Valid PipelineTriggerSaveReqVO updateReqVO);

    /**
     * 删除流程触发配置表，管理定时和事件触发规则 [pd_]
     *
     * @param id 编号
     */
    void deletePipelineTrigger(Long id);

    /**
    * 批量删除流程触发配置表，管理定时和事件触发规则 [pd_]
    *
    * @param ids 编号
    */
    void deletePipelineTriggerListByIds(List<Long> ids);

    /**
     * 获得流程触发配置表，管理定时和事件触发规则 [pd_]
     *
     * @param id 编号
     * @return 流程触发配置表，管理定时和事件触发规则 [pd_]
     */
    PipelineTriggerDO getPipelineTrigger(Long id);

    /**
     * 获得流程触发配置表，管理定时和事件触发规则 [pd_]分页
     *
     * @param pageReqVO 分页查询
     * @return 流程触发配置表，管理定时和事件触发规则 [pd_]分页
     */
    PageResult<PipelineTriggerDO> getPipelineTriggerPage(PipelineTriggerPageReqVO pageReqVO);

}