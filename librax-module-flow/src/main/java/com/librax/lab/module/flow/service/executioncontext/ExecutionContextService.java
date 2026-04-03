package com.librax.lab.module.flow.service.executioncontext;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.executioncontext.vo.*;
import com.librax.lab.module.flow.dal.dataobject.executioncontext.ExecutionContextDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用 Service 接口
 *
 * @author 一南
 */
public interface ExecutionContextService {

    /**
     * 创建执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createExecutionContext(@Valid ExecutionContextSaveReqVO createReqVO);

    /**
     * 更新执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用
     *
     * @param updateReqVO 更新信息
     */
    void updateExecutionContext(@Valid ExecutionContextSaveReqVO updateReqVO);

    /**
     * 删除执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用
     *
     * @param id 编号
     */
    void deleteExecutionContext(Long id);

    /**
    * 批量删除执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用
    *
    * @param ids 编号
    */
    void deleteExecutionContextListByIds(List<Long> ids);

    /**
     * 获得执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用
     *
     * @param id 编号
     * @return 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用
     */
    ExecutionContextDO getExecutionContext(Long id);

    /**
     * 获得执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用分页
     *
     * @param pageReqVO 分页查询
     * @return 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用分页
     */
    PageResult<ExecutionContextDO> getExecutionContextPage(ExecutionContextPageReqVO pageReqVO);

}