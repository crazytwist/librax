package com.librax.lab.module.lab.service.sampleresult;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.sampleresult.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleResultDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.sample.SampleResultMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class SampleResultServiceImpl implements SampleResultService {

    @Resource
    private SampleResultMapper sampleResultMapper;

    @Override
    public Long createSampleResult(SampleResultSaveReqVO createReqVO) {
        // 插入
        SampleResultDO sampleResult = BeanUtils.toBean(createReqVO, SampleResultDO.class);
        sampleResultMapper.insert(sampleResult);

        // 返回
        return sampleResult.getId();
    }

    @Override
    public void updateSampleResult(SampleResultSaveReqVO updateReqVO) {
        // 校验存在
        validateSampleResultExists(updateReqVO.getId());
        // 更新
        SampleResultDO updateObj = BeanUtils.toBean(updateReqVO, SampleResultDO.class);
        sampleResultMapper.updateById(updateObj);
    }

    @Override
    public void deleteSampleResult(Long id) {
        // 校验存在
        validateSampleResultExists(id);
        // 删除
        sampleResultMapper.deleteById(id);
    }

    @Override
        public void deleteSampleResultListByIds(List<Long> ids) {
        // 删除
        sampleResultMapper.deleteByIds(ids);
        }


    private void validateSampleResultExists(Long id) {
        if (sampleResultMapper.selectById(id) == null) {
            throw exception(SAMPLE_RESULT_NOT_EXISTS);
        }
    }

    @Override
    public SampleResultDO getSampleResult(Long id) {
        return sampleResultMapper.selectById(id);
    }

    @Override
    public PageResult<SampleResultDO> getSampleResultPage(SampleResultPageReqVO pageReqVO) {
        return sampleResultMapper.selectPage(pageReqVO);
    }

}