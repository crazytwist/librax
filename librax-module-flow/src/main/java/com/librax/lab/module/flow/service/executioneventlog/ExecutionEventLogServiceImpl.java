package com.librax.lab.module.flow.service.executioneventlog;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.flow.controller.admin.executioneventlog.vo.*;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.executioneventlog.ExecutionEventLogMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 执行事件日志，只 INSERT 不修改，全链路追踪与审计 Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class ExecutionEventLogServiceImpl implements ExecutionEventLogService {

    @Resource
    private ExecutionEventLogMapper executionEventLogMapper;

    @Override
    public Long createExecutionEventLog(ExecutionEventLogSaveReqVO createReqVO) {
        // 插入
        ExecutionEventLogDO executionEventLog = BeanUtils.toBean(createReqVO, ExecutionEventLogDO.class);
        executionEventLogMapper.insert(executionEventLog);

        // 返回
        return executionEventLog.getId();
    }

    @Override
    public void updateExecutionEventLog(ExecutionEventLogSaveReqVO updateReqVO) {
        // 校验存在
        validateExecutionEventLogExists(updateReqVO.getId());
        // 更新
        ExecutionEventLogDO updateObj = BeanUtils.toBean(updateReqVO, ExecutionEventLogDO.class);
        executionEventLogMapper.updateById(updateObj);
    }

    @Override
    public void deleteExecutionEventLog(Long id) {
        // 校验存在
        validateExecutionEventLogExists(id);
        // 删除
        executionEventLogMapper.deleteById(id);
    }

    @Override
        public void deleteExecutionEventLogListByIds(List<Long> ids) {
        // 删除
        executionEventLogMapper.deleteByIds(ids);
        }


    private void validateExecutionEventLogExists(Long id) {
        if (executionEventLogMapper.selectById(id) == null) {
            throw exception(EXECUTION_EVENT_LOG_NOT_EXISTS);
        }
    }

    @Override
    public ExecutionEventLogDO getExecutionEventLog(Long id) {
        return executionEventLogMapper.selectById(id);
    }

    @Override
    public PageResult<ExecutionEventLogDO> getExecutionEventLogPage(ExecutionEventLogPageReqVO pageReqVO) {
        return executionEventLogMapper.selectPage(pageReqVO);
    }

}