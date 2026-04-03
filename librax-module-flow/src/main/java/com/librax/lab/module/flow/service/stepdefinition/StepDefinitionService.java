package com.librax.lab.module.flow.service.stepdefinition;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.stepdefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.stepdefinition.StepDefinitionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 步骤定义表，可复用的步骤组件库 [pd_] Service 接口
 *
 * @author 一南
 */
public interface StepDefinitionService {

    /**
     * 创建步骤定义表，可复用的步骤组件库 [pd_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStepDefinition(@Valid StepDefinitionSaveReqVO createReqVO);

    /**
     * 更新步骤定义表，可复用的步骤组件库 [pd_]
     *
     * @param updateReqVO 更新信息
     */
    void updateStepDefinition(@Valid StepDefinitionSaveReqVO updateReqVO);

    /**
     * 删除步骤定义表，可复用的步骤组件库 [pd_]
     *
     * @param id 编号
     */
    void deleteStepDefinition(Long id);

    /**
    * 批量删除步骤定义表，可复用的步骤组件库 [pd_]
    *
    * @param ids 编号
    */
    void deleteStepDefinitionListByIds(List<Long> ids);

    /**
     * 获得步骤定义表，可复用的步骤组件库 [pd_]
     *
     * @param id 编号
     * @return 步骤定义表，可复用的步骤组件库 [pd_]
     */
    StepDefinitionDO getStepDefinition(Long id);

    /**
     * 获得步骤定义表，可复用的步骤组件库 [pd_]分页
     *
     * @param pageReqVO 分页查询
     * @return 步骤定义表，可复用的步骤组件库 [pd_]分页
     */
    PageResult<StepDefinitionDO> getStepDefinitionPage(StepDefinitionPageReqVO pageReqVO);

}