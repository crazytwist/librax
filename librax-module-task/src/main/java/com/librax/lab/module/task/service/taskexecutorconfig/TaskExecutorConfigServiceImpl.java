package com.librax.lab.module.task.service.taskexecutorconfig;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.task.controller.admin.taskexecutorconfig.vo.*;
import com.librax.lab.module.task.dal.dataobject.taskexecutorconfig.TaskExecutorConfigDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.task.dal.mysql.taskexecutorconfig.TaskExecutorConfigMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.task.enums.ErrorCodeConstants.*;

/**
 * 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class TaskExecutorConfigServiceImpl implements TaskExecutorConfigService {

    @Resource
    private TaskExecutorConfigMapper executorConfigMapper;

    @Override
    public Long createExecutorConfig(TaskExecutorConfigSaveReqVO createReqVO) {
        // 插入
        TaskExecutorConfigDO executorConfig = BeanUtils.toBean(createReqVO, TaskExecutorConfigDO.class);
        executorConfigMapper.insert(executorConfig);

        // 返回
        return executorConfig.getId();
    }

    @Override
    public void updateExecutorConfig(TaskExecutorConfigSaveReqVO updateReqVO) {
        // 校验存在
        validateExecutorConfigExists(updateReqVO.getId());
        // 更新
        TaskExecutorConfigDO updateObj = BeanUtils.toBean(updateReqVO, TaskExecutorConfigDO.class);
        executorConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteExecutorConfig(Long id) {
        // 校验存在
        validateExecutorConfigExists(id);
        // 删除
        executorConfigMapper.deleteById(id);
    }

    @Override
        public void deleteExecutorConfigListByIds(List<Long> ids) {
        // 删除
        executorConfigMapper.deleteByIds(ids);
        }


    private void validateExecutorConfigExists(Long id) {
        if (executorConfigMapper.selectById(id) == null) {
            throw exception(EXECUTOR_CONFIG_NOT_EXISTS);
        }
    }

    @Override
    public TaskExecutorConfigDO getExecutorConfig(Long id) {
        return executorConfigMapper.selectById(id);
    }

    @Override
    public PageResult<TaskExecutorConfigDO> getExecutorConfigPage(TaskExecutorConfigPageReqVO pageReqVO) {
        return executorConfigMapper.selectPage(pageReqVO);
    }

}