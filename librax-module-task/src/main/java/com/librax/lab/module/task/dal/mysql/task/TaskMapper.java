package com.librax.lab.module.task.dal.mysql.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.enums.TaskStatusEnum;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.task.controller.admin.task.vo.*;

/**
 * 统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface TaskMapper extends BaseMapperX<TaskDO> {

    default PageResult<TaskDO> selectPage(TaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TaskDO>()
                .eqIfPresent(TaskDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(TaskDO::getTaskType, reqVO.getTaskType())
                .likeIfPresent(TaskDO::getTaskName, reqVO.getTaskName())
                .eqIfPresent(TaskDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(TaskDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(TaskDO::getStepKey, reqVO.getStepKey())
                .eqIfPresent(TaskDO::getSampleId, reqVO.getSampleId())
                .eqIfPresent(TaskDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(TaskDO::getPriority, reqVO.getPriority())
                .eqIfPresent(TaskDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(TaskDO::getScheduledAt, reqVO.getScheduledAt())
                .eqIfPresent(TaskDO::getDeadlineAt, reqVO.getDeadlineAt())
                .eqIfPresent(TaskDO::getExecutorId, reqVO.getExecutorId())
                .eqIfPresent(TaskDO::getExecutorType, reqVO.getExecutorType())
                .eqIfPresent(TaskDO::getExternalTaskId, reqVO.getExternalTaskId())
                .eqIfPresent(TaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TaskDO::getFailReason, reqVO.getFailReason())
                .eqIfPresent(TaskDO::getRetryCount, reqVO.getRetryCount())
                .eqIfPresent(TaskDO::getMaxRetry, reqVO.getMaxRetry())
                .eqIfPresent(TaskDO::getCallbackToken, reqVO.getCallbackToken())
                .eqIfPresent(TaskDO::getPayload, reqVO.getPayload())
                .eqIfPresent(TaskDO::getResult, reqVO.getResult())
                .eqIfPresent(TaskDO::getErrorCode, reqVO.getErrorCode())
                .eqIfPresent(TaskDO::getErrorMsg, reqVO.getErrorMsg())
                .eqIfPresent(TaskDO::getQueuedAt, reqVO.getQueuedAt())
                .eqIfPresent(TaskDO::getAssignedAt, reqVO.getAssignedAt())
                .eqIfPresent(TaskDO::getStartedAt, reqVO.getStartedAt())
                .eqIfPresent(TaskDO::getFinishedAt, reqVO.getFinishedAt())
                .eqIfPresent(TaskDO::getWaitMs, reqVO.getWaitMs())
                .eqIfPresent(TaskDO::getExecuteMs, reqVO.getExecuteMs())
                .betweenIfPresent(TaskDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TaskDO::getId));
    }

    // ── 查询 ──────────────────────────────────────────────────────

    default TaskDO selectByTaskId(String taskId) {
        return selectOne(TaskDO::getTaskId, taskId);
    }

    default TaskDO selectByCallbackToken(String callbackToken) {
        return selectOne(TaskDO::getCallbackToken, callbackToken);
    }

    /** 查所有活跃任务（ASSIGNED + EXECUTING），供超时扫描用 */
    default List<TaskDO> selectActiveTasks() {
        return selectList(new LambdaQueryWrapper<TaskDO>()
                .in(TaskDO::getStatus,
                        TaskStatusEnum.ASSIGNED.name(),
                        TaskStatusEnum.EXECUTING.name()));
    }

    /** 查超过 deadline 的 MANUAL 任务 */
    default List<TaskDO> selectOverdueManualTasks(LocalDateTime now) {
        return selectList(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTaskType, TaskTypeEnum.MANUAL.name())
                .in(TaskDO::getStatus,
                        TaskStatusEnum.ASSIGNED.name(),
                        TaskStatusEnum.EXECUTING.name())
                .isNotNull(TaskDO::getDeadlineAt)
                .lt(TaskDO::getDeadlineAt, now));
    }

    // ── 状态流转 ──────────────────────────────────────────────────

    /** PENDING：记录入队时间 */
    default int updateQueued(String taskId) {
        return update(new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTaskId, taskId)
                .set(TaskDO::getStatus, TaskStatusEnum.PENDING.name())
                .set(TaskDO::getQueuedAt, LocalDateTime.now())
                .set(TaskDO::getUpdateTime, LocalDateTime.now()));
    }

    /** PENDING → ASSIGNED：记录分配时间，计算等待耗时 */
    default int updateAssigned(String taskId) {
        LocalDateTime now = LocalDateTime.now();
        // 先查 queuedAt 计算 waitMs
        TaskDO task = selectByTaskId(taskId);
        long waitMs = (task != null && task.getQueuedAt() != null)
                ? Duration.between(task.getQueuedAt(), now).toMillis() : 0L;

        return update(new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTaskId, taskId)
                .eq(TaskDO::getStatus, TaskStatusEnum.PENDING.name())
                .set(TaskDO::getStatus, TaskStatusEnum.ASSIGNED.name())
                .set(TaskDO::getAssignedAt, now)
                .set(TaskDO::getWaitMs, waitMs)
                .set(TaskDO::getUpdateTime, now));
    }

    /** ASSIGNED → EXECUTING：记录外部任务ID和开始时间 */
    default int updateExecuting(String taskId, String externalTaskId) {
        LocalDateTime now = LocalDateTime.now();
        return update(new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTaskId, taskId)
                .set(TaskDO::getStatus, TaskStatusEnum.EXECUTING.name())
                .set(TaskDO::getExternalTaskId, externalTaskId)
                .set(TaskDO::getStartedAt, now)
                .set(TaskDO::getUpdateTime, now));
    }

    /** → DONE：记录结果和完成时间，计算执行耗时 */
    default int updateDone(String taskId, String result) {
        LocalDateTime now = LocalDateTime.now();
        TaskDO task = selectByTaskId(taskId);
        long executeMs = (task != null && task.getStartedAt() != null)
                ? Duration.between(task.getStartedAt(), now).toMillis() : 0L;

        return update(new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTaskId, taskId)
                .set(TaskDO::getStatus, TaskStatusEnum.DONE.name())
                .set(TaskDO::getResult, result)
                .set(TaskDO::getFinishedAt, now)
                .set(TaskDO::getExecuteMs, executeMs)
                .set(TaskDO::getUpdateTime, now));
    }

    /**
     * CAS 更新为 FAILED（防止和正常回调并发）
     * WHERE task_id = #{taskId} AND status = #{fromStatus}
     */
    default int casStatusToFailed(String taskId, String fromStatus,
                                  String errorCode, String errorMsg) {
        LocalDateTime now = LocalDateTime.now();
        TaskDO task = selectByTaskId(taskId);
        long executeMs = (task != null && task.getStartedAt() != null)
                ? Duration.between(task.getStartedAt(), now).toMillis() : 0L;

        return update(new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTaskId, taskId)
                .eq(TaskDO::getStatus, fromStatus)
                .set(TaskDO::getStatus, TaskStatusEnum.FAILED.name())
                .set(TaskDO::getErrorCode, errorCode)
                .set(TaskDO::getErrorMsg, errorMsg)
                .set(TaskDO::getFinishedAt, now)
                .set(TaskDO::getExecuteMs, executeMs)
                .set(TaskDO::getUpdateTime, now));
    }

    /**
     * 重试：retryCount+1，状态重置为 PENDING，清空执行相关字段
     * 保留 errorCode/errorMsg 供历史查询
     */
    default int incrementRetryAndReset(String taskId) {
        return update(new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTaskId, taskId)
                .setSql("retry_count = retry_count + 1")
                .set(TaskDO::getStatus, TaskStatusEnum.PENDING.name())
                .set(TaskDO::getExternalTaskId, null)
                .set(TaskDO::getExecutorId, null)
                .set(TaskDO::getAssignedAt, null)
                .set(TaskDO::getStartedAt, null)
                .set(TaskDO::getFinishedAt, null)
                .set(TaskDO::getExecuteMs, null)
                .set(TaskDO::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * CAS 更新任务为成功终态
     * 仅当当前状态匹配 fromStatus 时才更新,防止和 Watchdog 超时标记并发覆盖
     *
     * @return 影响行数,0 表示 CAS 失败
     */
    default int casUpdateDone(String taskId, String fromStatus, String result) {
        LocalDateTime now = LocalDateTime.now();
        return update(null, new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTaskId, taskId)
                .eq(TaskDO::getStatus, fromStatus)
                .set(TaskDO::getStatus, TaskStatusEnum.DONE.name())
                .set(TaskDO::getResult, result)
                .set(TaskDO::getFinishedAt, now));
        // 如需记录 executeMs,可在此补:SET execute_ms = TIMESTAMPDIFF(...)
        // 简单做法是在 dispatcher 里查 started_at 计算后 set 进来
    }
}