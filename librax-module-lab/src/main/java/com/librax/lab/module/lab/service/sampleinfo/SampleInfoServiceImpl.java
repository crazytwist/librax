package com.librax.lab.module.lab.service.sampleinfo;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.sampleinfo.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleInfoDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.sample.SampleInfoMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class SampleInfoServiceImpl implements SampleInfoService {

    @Resource
    private SampleInfoMapper sampleInfoMapper;

    @Override
    public Long createSampleInfo(SampleInfoSaveReqVO createReqVO) {
        // 插入
        SampleInfoDO sampleInfo = BeanUtils.toBean(createReqVO, SampleInfoDO.class);
        sampleInfoMapper.insert(sampleInfo);

        // 返回
        return sampleInfo.getId();
    }

    @Override
    public void updateSampleInfo(SampleInfoSaveReqVO updateReqVO) {
        // 校验存在
        validateSampleInfoExists(updateReqVO.getId());
        // 更新
        SampleInfoDO updateObj = BeanUtils.toBean(updateReqVO, SampleInfoDO.class);
        sampleInfoMapper.updateById(updateObj);
    }

    @Override
    public void deleteSampleInfo(Long id) {
        // 校验存在
        validateSampleInfoExists(id);
        // 删除
        sampleInfoMapper.deleteById(id);
    }

    @Override
        public void deleteSampleInfoListByIds(List<Long> ids) {
        // 删除
        sampleInfoMapper.deleteByIds(ids);
        }


    private void validateSampleInfoExists(Long id) {
        if (sampleInfoMapper.selectById(id) == null) {
            throw exception(SAMPLE_INFO_NOT_EXISTS);
        }
    }

    @Override
    public SampleInfoDO getSampleInfo(Long id) {
        return sampleInfoMapper.selectById(id);
    }

    @Override
    public PageResult<SampleInfoDO> getSampleInfoPage(SampleInfoPageReqVO pageReqVO) {
        return sampleInfoMapper.selectPage(pageReqVO);
    }

}