package com.librax.lab.module.lab.service.sampleevent;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.sampleevent.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleEventDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.sample.SampleEventMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class SampleEventServiceImpl implements SampleEventService {

    @Resource
    private SampleEventMapper sampleEventMapper;

    @Override
    public Long createSampleEvent(SampleEventSaveReqVO createReqVO) {
        // 插入
        SampleEventDO sampleEvent = BeanUtils.toBean(createReqVO, SampleEventDO.class);
        sampleEventMapper.insert(sampleEvent);

        // 返回
        return sampleEvent.getId();
    }

    @Override
    public void updateSampleEvent(SampleEventSaveReqVO updateReqVO) {
        // 校验存在
        validateSampleEventExists(updateReqVO.getId());
        // 更新
        SampleEventDO updateObj = BeanUtils.toBean(updateReqVO, SampleEventDO.class);
        sampleEventMapper.updateById(updateObj);
    }

    @Override
    public void deleteSampleEvent(Long id) {
        // 校验存在
        validateSampleEventExists(id);
        // 删除
        sampleEventMapper.deleteById(id);
    }

    @Override
        public void deleteSampleEventListByIds(List<Long> ids) {
        // 删除
        sampleEventMapper.deleteByIds(ids);
        }


    private void validateSampleEventExists(Long id) {
        if (sampleEventMapper.selectById(id) == null) {
            throw exception(SAMPLE_EVENT_NOT_EXISTS);
        }
    }

    @Override
    public SampleEventDO getSampleEvent(Long id) {
        return sampleEventMapper.selectById(id);
    }

    @Override
    public PageResult<SampleEventDO> getSampleEventPage(SampleEventPageReqVO pageReqVO) {
        return sampleEventMapper.selectPage(pageReqVO);
    }

}