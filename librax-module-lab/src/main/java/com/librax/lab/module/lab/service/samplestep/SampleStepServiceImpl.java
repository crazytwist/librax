package com.librax.lab.module.lab.service.samplestep;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.samplestep.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleStepDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.sample.SampleStepMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态 Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class SampleStepServiceImpl implements SampleStepService {

    @Resource
    private SampleStepMapper sampleStepMapper;

    @Override
    public Long createSampleStep(SampleStepSaveReqVO createReqVO) {
        // 插入
        SampleStepDO sampleStep = BeanUtils.toBean(createReqVO, SampleStepDO.class);
        sampleStepMapper.insert(sampleStep);

        // 返回
        return sampleStep.getId();
    }

    @Override
    public void updateSampleStep(SampleStepSaveReqVO updateReqVO) {
        // 校验存在
        validateSampleStepExists(updateReqVO.getId());
        // 更新
        SampleStepDO updateObj = BeanUtils.toBean(updateReqVO, SampleStepDO.class);
        sampleStepMapper.updateById(updateObj);
    }

    @Override
    public void deleteSampleStep(Long id) {
        // 校验存在
        validateSampleStepExists(id);
        // 删除
        sampleStepMapper.deleteById(id);
    }

    @Override
        public void deleteSampleStepListByIds(List<Long> ids) {
        // 删除
        sampleStepMapper.deleteByIds(ids);
        }


    private void validateSampleStepExists(Long id) {
        if (sampleStepMapper.selectById(id) == null) {
            throw exception(SAMPLE_STEP_NOT_EXISTS);
        }
    }

    @Override
    public SampleStepDO getSampleStep(Long id) {
        return sampleStepMapper.selectById(id);
    }

    @Override
    public PageResult<SampleStepDO> getSampleStepPage(SampleStepPageReqVO pageReqVO) {
        return sampleStepMapper.selectPage(pageReqVO);
    }

}