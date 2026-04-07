package com.librax.lab.module.lab.dal.mysql.sample;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleEventDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.sampleevent.vo.*;

import java.util.List;

/**
 * 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface SampleEventMapper extends BaseMapperX<SampleEventDO> {

    default PageResult<SampleEventDO> selectPage(SampleEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SampleEventDO>()
                .eqIfPresent(SampleEventDO::getSampleId, reqVO.getSampleId())
                .orderByDesc(SampleEventDO::getId));
    }

    default List<SampleEventDO> selectBySampleId(String sampleId) {
        return selectList(new LambdaQueryWrapperX<SampleEventDO>()
                .eq(SampleEventDO::getSampleId, sampleId)
                .orderByAsc(SampleEventDO::getOccurredAt));
    }

}