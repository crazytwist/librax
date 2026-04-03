package com.librax.lab.module.flow.service.stepexecution;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.flow.controller.admin.stepexecution.vo.*;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪 Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class StepExecutionServiceImpl implements StepExecutionService {

    @Resource
    private StepExecutionMapper stepExecutionMapper;

    @Override
    public Long createStepExecution(StepExecutionSaveReqVO createReqVO) {
        // 插入
        StepExecutionDO stepExecution = BeanUtils.toBean(createReqVO, StepExecutionDO.class);
        stepExecutionMapper.insert(stepExecution);

        // 返回
        return stepExecution.getId();
    }

    @Override
    public void updateStepExecution(StepExecutionSaveReqVO updateReqVO) {
        // 校验存在
        validateStepExecutionExists(updateReqVO.getId());
        // 更新
        StepExecutionDO updateObj = BeanUtils.toBean(updateReqVO, StepExecutionDO.class);
        stepExecutionMapper.updateById(updateObj);
    }

    @Override
    public void deleteStepExecution(Long id) {
        // 校验存在
        validateStepExecutionExists(id);
        // 删除
        stepExecutionMapper.deleteById(id);
    }

    @Override
        public void deleteStepExecutionListByIds(List<Long> ids) {
        // 删除
        stepExecutionMapper.deleteByIds(ids);
        }


    private void validateStepExecutionExists(Long id) {
        if (stepExecutionMapper.selectById(id) == null) {
            throw exception(STEP_EXECUTION_NOT_EXISTS);
        }
    }

    @Override
    public StepExecutionDO getStepExecution(Long id) {
        return stepExecutionMapper.selectById(id);
    }

    @Override
    public PageResult<StepExecutionDO> getStepExecutionPage(StepExecutionPageReqVO pageReqVO) {
        return stepExecutionMapper.selectPage(pageReqVO);
    }

}