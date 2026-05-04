package com.librax.lab.module.resource.service.stepresourcehold;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.resource.controller.admin.stepresourcehold.vo.*;
import com.librax.lab.module.resource.dal.dataobject.stepresourcehold.StepResourceHoldDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.resource.dal.mysql.stepresourcehold.StepResourceHoldMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.*;

/**
 * 步骤执行资源占用记录，released_at IS NULL 表示当前持有中 Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class StepResourceHoldServiceImpl implements StepResourceHoldService {

    @Resource
    private StepResourceHoldMapper stepResourceHoldMapper;

    @Override
    public Long createStepResourceHold(StepResourceHoldSaveReqVO createReqVO) {
        // 插入
        StepResourceHoldDO stepResourceHold = BeanUtils.toBean(createReqVO, StepResourceHoldDO.class);
        stepResourceHoldMapper.insert(stepResourceHold);

        // 返回
        return stepResourceHold.getId();
    }

    @Override
    public void updateStepResourceHold(StepResourceHoldSaveReqVO updateReqVO) {
        // 校验存在
        validateStepResourceHoldExists(updateReqVO.getId());
        // 更新
        StepResourceHoldDO updateObj = BeanUtils.toBean(updateReqVO, StepResourceHoldDO.class);
        stepResourceHoldMapper.updateById(updateObj);
    }

    @Override
    public void deleteStepResourceHold(Long id) {
        // 校验存在
        validateStepResourceHoldExists(id);
        // 删除
        stepResourceHoldMapper.deleteById(id);
    }

    @Override
        public void deleteStepResourceHoldListByIds(List<Long> ids) {
        // 删除
        stepResourceHoldMapper.deleteByIds(ids);
        }


    private void validateStepResourceHoldExists(Long id) {
        if (stepResourceHoldMapper.selectById(id) == null) {
            throw exception(STEP_RESOURCE_HOLD_NOT_EXISTS);
        }
    }

    @Override
    public StepResourceHoldDO getStepResourceHold(Long id) {
        return stepResourceHoldMapper.selectById(id);
    }

    @Override
    public PageResult<StepResourceHoldDO> getStepResourceHoldPage(StepResourceHoldPageReqVO pageReqVO) {
        return stepResourceHoldMapper.selectPage(pageReqVO);
    }

}