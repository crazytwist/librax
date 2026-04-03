package com.librax.lab.module.flow.service.executioneventlog;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.executioneventlog.vo.*;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 执行事件日志，只 INSERT 不修改，全链路追踪与审计 Service 接口
 *
 * @author 一南
 */
public interface ExecutionEventLogService {

    /**
     * 创建执行事件日志，只 INSERT 不修改，全链路追踪与审计
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createExecutionEventLog(@Valid ExecutionEventLogSaveReqVO createReqVO);

    /**
     * 更新执行事件日志，只 INSERT 不修改，全链路追踪与审计
     *
     * @param updateReqVO 更新信息
     */
    void updateExecutionEventLog(@Valid ExecutionEventLogSaveReqVO updateReqVO);

    /**
     * 删除执行事件日志，只 INSERT 不修改，全链路追踪与审计
     *
     * @param id 编号
     */
    void deleteExecutionEventLog(Long id);

    /**
    * 批量删除执行事件日志，只 INSERT 不修改，全链路追踪与审计
    *
    * @param ids 编号
    */
    void deleteExecutionEventLogListByIds(List<Long> ids);

    /**
     * 获得执行事件日志，只 INSERT 不修改，全链路追踪与审计
     *
     * @param id 编号
     * @return 执行事件日志，只 INSERT 不修改，全链路追踪与审计
     */
    ExecutionEventLogDO getExecutionEventLog(Long id);

    /**
     * 获得执行事件日志，只 INSERT 不修改，全链路追踪与审计分页
     *
     * @param pageReqVO 分页查询
     * @return 执行事件日志，只 INSERT 不修改，全链路追踪与审计分页
     */
    PageResult<ExecutionEventLogDO> getExecutionEventLogPage(ExecutionEventLogPageReqVO pageReqVO);

}