package com.librax.lab.module.resource.service.stepresourcereq;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.resource.controller.admin.stepresourcereq.vo.*;
import com.librax.lab.module.resource.dal.dataobject.stepresourcereq.StepResourceReqDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 步骤资源需求定义，一个步骤节点可配多行（一步多资源） Service 接口
 *
 * @author 一南
 */
public interface StepResourceReqService {

    /**
     * 创建步骤资源需求定义，一个步骤节点可配多行（一步多资源）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStepResourceReq(@Valid StepResourceReqSaveReqVO createReqVO);

    /**
     * 更新步骤资源需求定义，一个步骤节点可配多行（一步多资源）
     *
     * @param updateReqVO 更新信息
     */
    void updateStepResourceReq(@Valid StepResourceReqSaveReqVO updateReqVO);

    /**
     * 删除步骤资源需求定义，一个步骤节点可配多行（一步多资源）
     *
     * @param id 编号
     */
    void deleteStepResourceReq(Long id);

    /**
    * 批量删除步骤资源需求定义，一个步骤节点可配多行（一步多资源）
    *
    * @param ids 编号
    */
    void deleteStepResourceReqListByIds(List<Long> ids);

    /**
     * 获得步骤资源需求定义，一个步骤节点可配多行（一步多资源）
     *
     * @param id 编号
     * @return 步骤资源需求定义，一个步骤节点可配多行（一步多资源）
     */
    StepResourceReqDO getStepResourceReq(Long id);

    /**
     * 获得步骤资源需求定义，一个步骤节点可配多行（一步多资源）分页
     *
     * @param pageReqVO 分页查询
     * @return 步骤资源需求定义，一个步骤节点可配多行（一步多资源）分页
     */
    PageResult<StepResourceReqDO> getStepResourceReqPage(StepResourceReqPageReqVO pageReqVO);

}