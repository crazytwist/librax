package com.librax.lab.module.flow.service.stepdefinition;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.flow.controller.admin.stepdefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.stepdefinition.StepDefinitionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.stepdefinition.StepDefinitionMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 步骤定义表，可复用的步骤组件库 [pd_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class StepDefinitionServiceImpl implements StepDefinitionService {

    @Resource
    private StepDefinitionMapper stepDefinitionMapper;

    @Override
    public Long createStepDefinition(StepDefinitionSaveReqVO createReqVO) {
        // 插入
        StepDefinitionDO stepDefinition = BeanUtils.toBean(createReqVO, StepDefinitionDO.class);
        stepDefinitionMapper.insert(stepDefinition);

        // 返回
        return stepDefinition.getId();
    }

    @Override
    public void updateStepDefinition(StepDefinitionSaveReqVO updateReqVO) {
        // 校验存在
        validateStepDefinitionExists(updateReqVO.getId());
        // 更新
        StepDefinitionDO updateObj = BeanUtils.toBean(updateReqVO, StepDefinitionDO.class);
        stepDefinitionMapper.updateById(updateObj);
    }

    @Override
    public void deleteStepDefinition(Long id) {
        // 校验存在
        validateStepDefinitionExists(id);
        // 删除
        stepDefinitionMapper.deleteById(id);
    }

    @Override
        public void deleteStepDefinitionListByIds(List<Long> ids) {
        // 删除
        stepDefinitionMapper.deleteByIds(ids);
        }


    private void validateStepDefinitionExists(Long id) {
        if (stepDefinitionMapper.selectById(id) == null) {
            throw exception(STEP_DEFINITION_NOT_EXISTS);
        }
    }

    @Override
    public StepDefinitionDO getStepDefinition(Long id) {
        return stepDefinitionMapper.selectById(id);
    }

    @Override
    public PageResult<StepDefinitionDO> getStepDefinitionPage(StepDefinitionPageReqVO pageReqVO) {
        return stepDefinitionMapper.selectPage(pageReqVO);
    }

}