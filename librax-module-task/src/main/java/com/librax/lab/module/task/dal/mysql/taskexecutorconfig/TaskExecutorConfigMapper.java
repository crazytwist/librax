package com.librax.lab.module.task.dal.mysql.taskexecutorconfig;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.task.dal.dataobject.taskexecutorconfig.TaskExecutorConfigDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.task.controller.admin.taskexecutorconfig.vo.*;

/**
 * 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface TaskExecutorConfigMapper extends BaseMapperX<TaskExecutorConfigDO> {

    default PageResult<TaskExecutorConfigDO> selectPage(TaskExecutorConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TaskExecutorConfigDO>()
                .eqIfPresent(TaskExecutorConfigDO::getExecutorType, reqVO.getExecutorType())
                .eqIfPresent(TaskExecutorConfigDO::getMaxConcurrent, reqVO.getMaxConcurrent())
                .eqIfPresent(TaskExecutorConfigDO::getQueueCapacity, reqVO.getQueueCapacity())
                .eqIfPresent(TaskExecutorConfigDO::getTaskTimeoutMs, reqVO.getTaskTimeoutMs())
                .eqIfPresent(TaskExecutorConfigDO::getRetryBackoffMs, reqVO.getRetryBackoffMs())
                .eqIfPresent(TaskExecutorConfigDO::getEnabled, reqVO.getEnabled())
                .eqIfPresent(TaskExecutorConfigDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(TaskExecutorConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TaskExecutorConfigDO::getId));
    }

}