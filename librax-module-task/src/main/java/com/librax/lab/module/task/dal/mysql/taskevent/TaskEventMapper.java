package com.librax.lab.module.task.dal.mysql.taskevent;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.task.dal.dataobject.taskevent.TaskEventDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.task.controller.admin.taskevent.vo.*;

/**
 * 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface TaskEventMapper extends BaseMapperX<TaskEventDO> {

    default PageResult<TaskEventDO> selectPage(TaskEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TaskEventDO>()
                .eqIfPresent(TaskEventDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(TaskEventDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(TaskEventDO::getEventType, reqVO.getEventType())
                .eqIfPresent(TaskEventDO::getFromStatus, reqVO.getFromStatus())
                .eqIfPresent(TaskEventDO::getToStatus, reqVO.getToStatus())
                .eqIfPresent(TaskEventDO::getExecutorId, reqVO.getExecutorId())
                .eqIfPresent(TaskEventDO::getPayload, reqVO.getPayload())
                .eqIfPresent(TaskEventDO::getOperator, reqVO.getOperator())
                .eqIfPresent(TaskEventDO::getOccurredAt, reqVO.getOccurredAt())
                .betweenIfPresent(TaskEventDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TaskEventDO::getId));
    }

}