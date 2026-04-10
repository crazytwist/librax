package com.librax.lab.module.lab.dal.mysql.sample;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleInfoDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.sampleinfo.vo.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联 Mapper
 *
 * @author 一南
 */
@Mapper
public interface SampleInfoMapper extends BaseMapperX<SampleInfoDO> {

    default PageResult<SampleInfoDO> selectPage(SampleInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SampleInfoDO>()
                .eqIfPresent(SampleInfoDO::getSampleId, reqVO.getSampleId())
                .eqIfPresent(SampleInfoDO::getDeriveType, reqVO.getDeriveType())
                .eqIfPresent(SampleInfoDO::getSampleType, reqVO.getSampleType())
                .likeIfPresent(SampleInfoDO::getSampleName, reqVO.getSampleName())
                .eqIfPresent(SampleInfoDO::getContainerCode, reqVO.getContainerCode())
                .eqIfPresent(SampleInfoDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SampleInfoDO::getLocationCode, reqVO.getLocationCode())
                .eqIfPresent(SampleInfoDO::getBatchNo, reqVO.getBatchNo())
                .betweenIfPresent(SampleInfoDO::getExpireTime, reqVO.getExpireTime())
                .betweenIfPresent(SampleInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SampleInfoDO::getId));
    }


    default SampleInfoDO selectBySampleId(String sampleId) {
        return selectOne(SampleInfoDO::getSampleId, sampleId);
    }

    default List<SampleInfoDO> selectByBatchNo(String batchNo) {
        return selectList(SampleInfoDO::getBatchNo, batchNo);
    }

    default List<SampleInfoDO> selectByRootSampleId(String rootSampleId) {
        return selectList(SampleInfoDO::getRootSampleId, rootSampleId);
    }

    default int updateStatusBySampleId(String sampleId, String status,
                                       String executionId, String nodeId) {
        return update(null, new LambdaUpdateWrapper<SampleInfoDO>()
                .eq(SampleInfoDO::getSampleId, sampleId)
                .set(SampleInfoDO::getStatus, status)
                .set(SampleInfoDO::getCurrentExecutionId, executionId)
                .set(SampleInfoDO::getCurrentNodeId, nodeId)
                .set(SampleInfoDO::getUpdateTime, LocalDateTime.now()));
    }

}