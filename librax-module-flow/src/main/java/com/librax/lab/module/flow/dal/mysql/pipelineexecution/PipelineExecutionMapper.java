package com.librax.lab.module.flow.dal.mysql.pipelineexecution;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.pipelineexecution.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 流程执行实例，支持完整流程、节点单独运行、补偿执行 Mapper
 *
 * @author 一南
 */
@Mapper
public interface PipelineExecutionMapper extends BaseMapperX<PipelineExecutionDO> {


    PipelineExecutionDO selectByExecutionId(@Param("executionId") String executionId);

    int compareAndSetStatus(@Param("executionId") String executionId,
                            @Param("fromStatus") String fromStatus,
                            @Param("toStatus") String toStatus);

    List<PipelineExecutionDO> selectAllRunning();


    default PageResult<PipelineExecutionDO> selectPage(PipelineExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PipelineExecutionDO>()
                .eqIfPresent(PipelineExecutionDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(PipelineExecutionDO::getPipelineKey, reqVO.getPipelineKey())
                .eqIfPresent(PipelineExecutionDO::getPipelineVersion, reqVO.getPipelineVersion())
                .eqIfPresent(PipelineExecutionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PipelineExecutionDO::getTriggerType, reqVO.getTriggerType())
                .eqIfPresent(PipelineExecutionDO::getTriggeredBy, reqVO.getTriggeredBy())
                .eqIfPresent(PipelineExecutionDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(PipelineExecutionDO::getInputParams, reqVO.getInputParams())
                .eqIfPresent(PipelineExecutionDO::getParentExecutionId, reqVO.getParentExecutionId())
                .eqIfPresent(PipelineExecutionDO::getStandaloneNodeId, reqVO.getStandaloneNodeId())
                .eqIfPresent(PipelineExecutionDO::getOriginExecutionId, reqVO.getOriginExecutionId())
                .eqIfPresent(PipelineExecutionDO::getStartedAt, reqVO.getStartedAt())
                .eqIfPresent(PipelineExecutionDO::getFinishedAt, reqVO.getFinishedAt())
                .eqIfPresent(PipelineExecutionDO::getTotalMs, reqVO.getTotalMs())
                .eqIfPresent(PipelineExecutionDO::getCriticalPath, reqVO.getCriticalPath())
                .eqIfPresent(PipelineExecutionDO::getRowVersion, reqVO.getRowVersion())
                .betweenIfPresent(PipelineExecutionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PipelineExecutionDO::getId));
    }

}