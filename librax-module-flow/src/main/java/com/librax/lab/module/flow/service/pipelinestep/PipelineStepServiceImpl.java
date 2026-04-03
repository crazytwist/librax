package com.librax.lab.module.flow.service.pipelinestep;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.flow.controller.admin.pipelinestep.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinestep.PipelineStepDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.pipelinestep.PipelineStepMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class PipelineStepServiceImpl implements PipelineStepService {

    @Resource
    private PipelineStepMapper pipelineStepMapper;

    @Override
    public Long createPipelineStep(PipelineStepSaveReqVO createReqVO) {
        // 插入
        PipelineStepDO pipelineStep = BeanUtils.toBean(createReqVO, PipelineStepDO.class);
        pipelineStepMapper.insert(pipelineStep);

        // 返回
        return pipelineStep.getId();
    }

    @Override
    public void updatePipelineStep(PipelineStepSaveReqVO updateReqVO) {
        // 校验存在
        validatePipelineStepExists(updateReqVO.getId());
        // 更新
        PipelineStepDO updateObj = BeanUtils.toBean(updateReqVO, PipelineStepDO.class);
        pipelineStepMapper.updateById(updateObj);
    }

    @Override
    public void deletePipelineStep(Long id) {
        // 校验存在
        validatePipelineStepExists(id);
        // 删除
        pipelineStepMapper.deleteById(id);
    }

    @Override
        public void deletePipelineStepListByIds(List<Long> ids) {
        // 删除
        pipelineStepMapper.deleteByIds(ids);
        }


    private void validatePipelineStepExists(Long id) {
        if (pipelineStepMapper.selectById(id) == null) {
            throw exception(PIPELINE_STEP_NOT_EXISTS);
        }
    }

    @Override
    public PipelineStepDO getPipelineStep(Long id) {
        return pipelineStepMapper.selectById(id);
    }

    @Override
    public PageResult<PipelineStepDO> getPipelineStepPage(PipelineStepPageReqVO pageReqVO) {
        return pipelineStepMapper.selectPage(pageReqVO);
    }

}