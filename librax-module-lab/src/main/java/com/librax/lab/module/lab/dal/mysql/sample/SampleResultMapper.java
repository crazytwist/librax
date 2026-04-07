package com.librax.lab.module.lab.dal.mysql.sample;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleResultDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.sampleresult.vo.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface SampleResultMapper extends BaseMapperX<SampleResultDO> {

    default PageResult<SampleResultDO> selectPage(SampleResultPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SampleResultDO>()
                .eqIfPresent(SampleResultDO::getSampleId, reqVO.getSampleId())
                .likeIfPresent(SampleResultDO::getDeviceName, reqVO.getDeviceName())
                .betweenIfPresent(SampleResultDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SampleResultDO::getId));
    }

    default List<SampleResultDO> selectBySampleId(String sampleId) {
        return selectList(new LambdaQueryWrapperX<SampleResultDO>()
                .eq(SampleResultDO::getSampleId, sampleId)
                .eq(SampleResultDO::getIsFinal, true)
                .orderByAsc(SampleResultDO::getTestItem));
    }

    default List<SampleResultDO> selectBySampleAndItem(String sampleId, String testItem) {
        return selectList(new LambdaQueryWrapperX<SampleResultDO>()
                .eq(SampleResultDO::getSampleId, sampleId)
                .eq(SampleResultDO::getTestItem, testItem)
                .orderByDesc(SampleResultDO::getCreateTime));
    }

    default List<SampleResultDO> selectByExecutionAndNode(String executionId, String nodeId) {
        return selectList(new LambdaQueryWrapperX<SampleResultDO>()
                .eq(SampleResultDO::getExecutionId, executionId)
                .eq(SampleResultDO::getNodeId, nodeId));
    }

    default SampleResultDO selectByResultId(String resultId) {
        return selectOne(SampleResultDO::getResultId, resultId);
    }

    default List<SampleResultDO> selectPendingReview() {
        return selectList(new LambdaQueryWrapperX<SampleResultDO>()
                .eq(SampleResultDO::getReviewStatus, "PENDING")
                .eq(SampleResultDO::getIsFinal, true)
                .orderByAsc(SampleResultDO::getCreateTime));
    }

    default List<SampleResultDO> selectAbnormalByBatch(String batchNo) {
        return selectList(new LambdaQueryWrapperX<SampleResultDO>()
                .eq(SampleResultDO::getBatchNo, batchNo)
                .eq(SampleResultDO::getIsAbnormal, true)
                .eq(SampleResultDO::getIsFinal, true));
    }

    default int updateReviewStatus(String resultId, String reviewStatus,
                                   String reviewedBy, String comment) {
        return update(null, new LambdaUpdateWrapper<SampleResultDO>()
                .eq(SampleResultDO::getResultId, resultId)
                .set(SampleResultDO::getReviewStatus, reviewStatus)
                .set(SampleResultDO::getReviewedBy, reviewedBy)
                .set(SampleResultDO::getReviewedAt, LocalDateTime.now())
                .set(SampleResultDO::getReviewComment, comment)
                .set(SampleResultDO::getUpdateTime, LocalDateTime.now()));
    }

}