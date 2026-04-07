package com.librax.lab.module.lab.service.samplerelation;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.samplerelation.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleRelationDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.sample.SampleRelationMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 样本谱系关系表，记录拆分/合并/分装等衍生关系 Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class SampleRelationServiceImpl implements SampleRelationService {

    @Resource
    private SampleRelationMapper sampleRelationMapper;

    @Override
    public Long createSampleRelation(SampleRelationSaveReqVO createReqVO) {
        // 插入
        SampleRelationDO sampleRelation = BeanUtils.toBean(createReqVO, SampleRelationDO.class);
        sampleRelationMapper.insert(sampleRelation);

        // 返回
        return sampleRelation.getId();
    }

    @Override
    public void updateSampleRelation(SampleRelationSaveReqVO updateReqVO) {
        // 校验存在
        validateSampleRelationExists(updateReqVO.getId());
        // 更新
        SampleRelationDO updateObj = BeanUtils.toBean(updateReqVO, SampleRelationDO.class);
        sampleRelationMapper.updateById(updateObj);
    }

    @Override
    public void deleteSampleRelation(Long id) {
        // 校验存在
        validateSampleRelationExists(id);
        // 删除
        sampleRelationMapper.deleteById(id);
    }

    @Override
        public void deleteSampleRelationListByIds(List<Long> ids) {
        // 删除
        sampleRelationMapper.deleteByIds(ids);
        }


    private void validateSampleRelationExists(Long id) {
        if (sampleRelationMapper.selectById(id) == null) {
            throw exception(SAMPLE_RELATION_NOT_EXISTS);
        }
    }

    @Override
    public SampleRelationDO getSampleRelation(Long id) {
        return sampleRelationMapper.selectById(id);
    }

    @Override
    public PageResult<SampleRelationDO> getSampleRelationPage(SampleRelationPageReqVO pageReqVO) {
        return sampleRelationMapper.selectPage(pageReqVO);
    }

}