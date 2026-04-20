package com.librax.lab.module.task.service.taskexecutorconfig;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.task.controller.admin.taskexecutorconfig.vo.*;
import com.librax.lab.module.task.dal.dataobject.taskexecutorconfig.TaskExecutorConfigDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_] Service 接口
 *
 * @author 一南
 */
public interface TaskExecutorConfigService {

    /**
     * 创建执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createExecutorConfig(@Valid TaskExecutorConfigSaveReqVO createReqVO);

    /**
     * 更新执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]
     *
     * @param updateReqVO 更新信息
     */
    void updateExecutorConfig(@Valid TaskExecutorConfigSaveReqVO updateReqVO);

    /**
     * 删除执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]
     *
     * @param id 编号
     */
    void deleteExecutorConfig(Long id);

    /**
    * 批量删除执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]
    *
    * @param ids 编号
    */
    void deleteExecutorConfigListByIds(List<Long> ids);

    /**
     * 获得执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]
     *
     * @param id 编号
     * @return 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]
     */
    TaskExecutorConfigDO getExecutorConfig(Long id);

    /**
     * 获得执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]分页
     *
     * @param pageReqVO 分页查询
     * @return 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]分页
     */
    PageResult<TaskExecutorConfigDO> getExecutorConfigPage(TaskExecutorConfigPageReqVO pageReqVO);

}