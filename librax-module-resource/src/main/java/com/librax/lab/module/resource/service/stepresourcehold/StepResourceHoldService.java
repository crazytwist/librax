package com.librax.lab.module.resource.service.stepresourcehold;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.resource.controller.admin.stepresourcehold.vo.*;
import com.librax.lab.module.resource.dal.dataobject.stepresourcehold.StepResourceHoldDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 步骤执行资源占用记录，released_at IS NULL 表示当前持有中 Service 接口
 *
 * @author 一南
 */
public interface StepResourceHoldService {

    /**
     * 创建步骤执行资源占用记录，released_at IS NULL 表示当前持有中
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStepResourceHold(@Valid StepResourceHoldSaveReqVO createReqVO);

    /**
     * 更新步骤执行资源占用记录，released_at IS NULL 表示当前持有中
     *
     * @param updateReqVO 更新信息
     */
    void updateStepResourceHold(@Valid StepResourceHoldSaveReqVO updateReqVO);

    /**
     * 删除步骤执行资源占用记录，released_at IS NULL 表示当前持有中
     *
     * @param id 编号
     */
    void deleteStepResourceHold(Long id);

    /**
    * 批量删除步骤执行资源占用记录，released_at IS NULL 表示当前持有中
    *
    * @param ids 编号
    */
    void deleteStepResourceHoldListByIds(List<Long> ids);

    /**
     * 获得步骤执行资源占用记录，released_at IS NULL 表示当前持有中
     *
     * @param id 编号
     * @return 步骤执行资源占用记录，released_at IS NULL 表示当前持有中
     */
    StepResourceHoldDO getStepResourceHold(Long id);

    /**
     * 获得步骤执行资源占用记录，released_at IS NULL 表示当前持有中分页
     *
     * @param pageReqVO 分页查询
     * @return 步骤执行资源占用记录，released_at IS NULL 表示当前持有中分页
     */
    PageResult<StepResourceHoldDO> getStepResourceHoldPage(StepResourceHoldPageReqVO pageReqVO);

}