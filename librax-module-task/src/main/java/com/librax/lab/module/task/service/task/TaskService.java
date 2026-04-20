package com.librax.lab.module.task.service.task;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.task.controller.admin.task.vo.*;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_] Service 接口
 *
 * @author 一南
 */
public interface TaskService {

    /**
     * 创建统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTask(@Valid TaskSaveReqVO createReqVO);

    /**
     * 更新统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]
     *
     * @param updateReqVO 更新信息
     */
    void updateTask(@Valid TaskSaveReqVO updateReqVO);

    /**
     * 删除统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]
     *
     * @param id 编号
     */
    void deleteTask(Long id);

    /**
    * 批量删除统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]
    *
    * @param ids 编号
    */
    void deleteTaskListByIds(List<Long> ids);

    /**
     * 获得统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]
     *
     * @param id 编号
     * @return 统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]
     */
    TaskDO getTask(Long id);

    /**
     * 获得统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]分页
     *
     * @param pageReqVO 分页查询
     * @return 统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]分页
     */
    PageResult<TaskDO> getTaskPage(TaskPageReqVO pageReqVO);

}