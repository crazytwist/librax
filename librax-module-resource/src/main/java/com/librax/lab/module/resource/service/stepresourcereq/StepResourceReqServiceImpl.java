package com.librax.lab.module.resource.service.stepresourcereq;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.resource.controller.admin.stepresourcereq.vo.*;
import com.librax.lab.module.resource.dal.dataobject.stepresourcereq.StepResourceReqDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.resource.dal.mysql.stepresourcereq.StepResourceReqMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.*;

/**
 * 步骤资源需求定义，一个步骤节点可配多行（一步多资源） Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class StepResourceReqServiceImpl implements StepResourceReqService {

    @Resource
    private StepResourceReqMapper stepResourceReqMapper;

    @Override
    public Long createStepResourceReq(StepResourceReqSaveReqVO createReqVO) {
        // 插入
        StepResourceReqDO stepResourceReq = BeanUtils.toBean(createReqVO, StepResourceReqDO.class);
        stepResourceReqMapper.insert(stepResourceReq);

        // 返回
        return stepResourceReq.getId();
    }

    @Override
    public void updateStepResourceReq(StepResourceReqSaveReqVO updateReqVO) {
        // 校验存在
        validateStepResourceReqExists(updateReqVO.getId());
        // 更新
        StepResourceReqDO updateObj = BeanUtils.toBean(updateReqVO, StepResourceReqDO.class);
        stepResourceReqMapper.updateById(updateObj);
    }

    @Override
    public void deleteStepResourceReq(Long id) {
        // 校验存在
        validateStepResourceReqExists(id);
        // 删除
        stepResourceReqMapper.deleteById(id);
    }

    @Override
        public void deleteStepResourceReqListByIds(List<Long> ids) {
        // 删除
        stepResourceReqMapper.deleteByIds(ids);
        }


    private void validateStepResourceReqExists(Long id) {
        if (stepResourceReqMapper.selectById(id) == null) {
            throw exception(STEP_RESOURCE_REQ_NOT_EXISTS);
        }
    }

    @Override
    public StepResourceReqDO getStepResourceReq(Long id) {
        return stepResourceReqMapper.selectById(id);
    }

    @Override
    public PageResult<StepResourceReqDO> getStepResourceReqPage(StepResourceReqPageReqVO pageReqVO) {
        return stepResourceReqMapper.selectPage(pageReqVO);
    }

}