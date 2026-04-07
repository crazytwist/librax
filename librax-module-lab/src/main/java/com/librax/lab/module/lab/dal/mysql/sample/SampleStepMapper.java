package com.librax.lab.module.lab.dal.mysql.sample;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleStepDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.samplestep.vo.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态 Mapper
 *
 * @author 一南
 */
@Mapper
public interface SampleStepMapper extends BaseMapperX<SampleStepDO> {

    default PageResult<SampleStepDO> selectPage(SampleStepPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SampleStepDO>()
                .eqIfPresent(SampleStepDO::getSampleId, reqVO.getSampleId())
                .eqIfPresent(SampleStepDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(SampleStepDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(SampleStepDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SampleStepDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SampleStepDO::getId));
    }


    default SampleStepDO selectBySampleAndStep(String sampleId,
                                               String executionId,
                                               String nodeId,
                                               int attempt) {
        return selectOne(new LambdaQueryWrapperX<SampleStepDO>()
                .eq(SampleStepDO::getSampleId, sampleId)
                .eq(SampleStepDO::getExecutionId, executionId)
                .eq(SampleStepDO::getNodeId, nodeId)
                .eq(SampleStepDO::getAttempt, attempt));
    }

    default List<SampleStepDO> selectBySampleId(String sampleId) {
        return selectList(new LambdaQueryWrapperX<SampleStepDO>()
                .eq(SampleStepDO::getSampleId, sampleId)
                .orderByAsc(SampleStepDO::getCreateTime));
    }

    default List<SampleStepDO> selectByExecutionAndNode(String executionId,
                                                        String nodeId) {
        return selectList(new LambdaQueryWrapperX<SampleStepDO>()
                .eq(SampleStepDO::getExecutionId, executionId)
                .eq(SampleStepDO::getNodeId, nodeId)
                .orderByAsc(SampleStepDO::getSeqNo));
    }

    default int updateStatus(String sampleId, String executionId,
                             String nodeId, int attempt, String status) {
        return update(null, new LambdaUpdateWrapper<SampleStepDO>()
                .eq(SampleStepDO::getSampleId, sampleId)
                .eq(SampleStepDO::getExecutionId, executionId)
                .eq(SampleStepDO::getNodeId, nodeId)
                .eq(SampleStepDO::getAttempt, attempt)
                .set(SampleStepDO::getStatus, status)
                .set(SampleStepDO::getUpdateTime, LocalDateTime.now()));
    }

}