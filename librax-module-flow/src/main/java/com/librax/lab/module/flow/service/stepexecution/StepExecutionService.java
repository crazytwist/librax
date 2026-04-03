package com.librax.lab.module.flow.service.stepexecution;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.stepexecution.vo.*;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪 Service 接口
 *
 * @author 一南
 */
public interface StepExecutionService {

    /**
     * 创建步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStepExecution(@Valid StepExecutionSaveReqVO createReqVO);

    /**
     * 更新步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪
     *
     * @param updateReqVO 更新信息
     */
    void updateStepExecution(@Valid StepExecutionSaveReqVO updateReqVO);

    /**
     * 删除步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪
     *
     * @param id 编号
     */
    void deleteStepExecution(Long id);

    /**
    * 批量删除步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪
    *
    * @param ids 编号
    */
    void deleteStepExecutionListByIds(List<Long> ids);

    /**
     * 获得步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪
     *
     * @param id 编号
     * @return 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪
     */
    StepExecutionDO getStepExecution(Long id);

    /**
     * 获得步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪分页
     *
     * @param pageReqVO 分页查询
     * @return 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪分页
     */
    PageResult<StepExecutionDO> getStepExecutionPage(StepExecutionPageReqVO pageReqVO);

}