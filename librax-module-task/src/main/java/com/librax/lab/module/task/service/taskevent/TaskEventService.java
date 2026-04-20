package com.librax.lab.module.task.service.taskevent;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.task.controller.admin.taskevent.vo.*;
import com.librax.lab.module.task.dal.dataobject.taskevent.TaskEventDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_] Service 接口
 *
 * @author 一南
 */
public interface TaskEventService {

    /**
     * 创建任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEvent(@Valid TaskEventSaveReqVO createReqVO);

    /**
     * 更新任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]
     *
     * @param updateReqVO 更新信息
     */
    void updateEvent(@Valid TaskEventSaveReqVO updateReqVO);

    /**
     * 删除任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]
     *
     * @param id 编号
     */
    void deleteEvent(Long id);

    /**
    * 批量删除任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]
    *
    * @param ids 编号
    */
    void deleteEventListByIds(List<Long> ids);

    /**
     * 获得任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]
     *
     * @param id 编号
     * @return 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]
     */
    TaskEventDO getEvent(Long id);

    /**
     * 获得任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]分页
     *
     * @param pageReqVO 分页查询
     * @return 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]分页
     */
    PageResult<TaskEventDO> getEventPage(TaskEventPageReqVO pageReqVO);

}