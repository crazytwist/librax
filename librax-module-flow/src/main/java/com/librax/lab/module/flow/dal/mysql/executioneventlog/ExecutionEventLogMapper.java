package com.librax.lab.module.flow.dal.mysql.executioneventlog;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.executioneventlog.vo.*;

/**
 * 执行事件日志，只 INSERT 不修改，全链路追踪与审计 Mapper
 *
 * @author 一南
 */
@Mapper
public interface ExecutionEventLogMapper extends BaseMapperX<ExecutionEventLogDO> {

    default PageResult<ExecutionEventLogDO> selectPage(ExecutionEventLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ExecutionEventLogDO>()
                .eqIfPresent(ExecutionEventLogDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(ExecutionEventLogDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(ExecutionEventLogDO::getAttempt, reqVO.getAttempt())
                .eqIfPresent(ExecutionEventLogDO::getRunMode, reqVO.getRunMode())
                .eqIfPresent(ExecutionEventLogDO::getEventType, reqVO.getEventType())
                .eqIfPresent(ExecutionEventLogDO::getFromStatus, reqVO.getFromStatus())
                .eqIfPresent(ExecutionEventLogDO::getToStatus, reqVO.getToStatus())
                .eqIfPresent(ExecutionEventLogDO::getPayload, reqVO.getPayload())
                .eqIfPresent(ExecutionEventLogDO::getOperator, reqVO.getOperator())
                .eqIfPresent(ExecutionEventLogDO::getOccurredAt, reqVO.getOccurredAt())
                .betweenIfPresent(ExecutionEventLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ExecutionEventLogDO::getId));
    }

}