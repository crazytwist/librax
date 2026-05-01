package com.librax.lab.module.flow.dal.mysql.stepexecution;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.stepexecution.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪 Mapper
 *
 * @author 一南
 */
@Mapper
public interface StepExecutionMapper extends BaseMapperX<StepExecutionDO> {

    default PageResult<StepExecutionDO> selectPage(StepExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StepExecutionDO>()
                .eqIfPresent(StepExecutionDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(StepExecutionDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(StepExecutionDO::getStepKey, reqVO.getStepKey())
                .eqIfPresent(StepExecutionDO::getStepType, reqVO.getStepType())
                .eqIfPresent(StepExecutionDO::getAttempt, reqVO.getAttempt())
                .eqIfPresent(StepExecutionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(StepExecutionDO::getRunMode, reqVO.getRunMode())
                .eqIfPresent(StepExecutionDO::getInputSnapshot, reqVO.getInputSnapshot())
                .eqIfPresent(StepExecutionDO::getOutputData, reqVO.getOutputData())
                .eqIfPresent(StepExecutionDO::getErrorCode, reqVO.getErrorCode())
                .eqIfPresent(StepExecutionDO::getErrorMsg, reqVO.getErrorMsg())
                .eqIfPresent(StepExecutionDO::getQueuedAt, reqVO.getQueuedAt())
                .eqIfPresent(StepExecutionDO::getStartedAt, reqVO.getStartedAt())
                .eqIfPresent(StepExecutionDO::getFinishedAt, reqVO.getFinishedAt())
                .eqIfPresent(StepExecutionDO::getWaitMs, reqVO.getWaitMs())
                .eqIfPresent(StepExecutionDO::getExecuteMs, reqVO.getExecuteMs())
                .eqIfPresent(StepExecutionDO::getDeviceId, reqVO.getDeviceId())
                .eqIfPresent(StepExecutionDO::getCompensateExecutionId, reqVO.getCompensateExecutionId())
                .betweenIfPresent(StepExecutionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(StepExecutionDO::getId));
    }


    StepExecutionDO selectLatestAttempt(@Param("executionId") String executionId,
                                        @Param("nodeId") String nodeId);


    StepExecutionDO selectByExecutionNodeAttempt(
            @Param("executionId") String executionId,
            @Param("nodeId") String nodeId,
            @Param("attempt") int attempt);


    List<StepExecutionDO> selectLatestByExecutionId(String executionId);


    default void resetToPending(String executionId, String nodeId) {
        update(new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .set(StepExecutionDO::getStatus, StepStatusEnum.PENDING.name()));
    }

    default void markResourceAcquired(String executionId, String nodeId, int attempt) {
        update(new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .set(StepExecutionDO::getResourceAcquired, 1));
    }

    default void clearResourceAcquired(String executionId, String nodeId, int attempt) {
        update(new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .set(StepExecutionDO::getResourceAcquired, 0));
    }
}