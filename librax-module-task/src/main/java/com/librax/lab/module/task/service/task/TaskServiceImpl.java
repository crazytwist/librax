package com.librax.lab.module.task.service.task;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.task.controller.admin.task.vo.*;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.task.dal.mysql.task.TaskMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.task.enums.ErrorCodeConstants.*;

/**
 * 统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class TaskServiceImpl implements TaskService {

    @Resource
    private TaskMapper taskMapper;

    @Override
    public Long createTask(TaskSaveReqVO createReqVO) {
        // 插入
        TaskDO task = BeanUtils.toBean(createReqVO, TaskDO.class);
        taskMapper.insert(task);

        // 返回
        return task.getId();
    }

    @Override
    public void updateTask(TaskSaveReqVO updateReqVO) {
        // 校验存在
        validateTaskExists(updateReqVO.getId());
        // 更新
        TaskDO updateObj = BeanUtils.toBean(updateReqVO, TaskDO.class);
        taskMapper.updateById(updateObj);
    }

    @Override
    public void deleteTask(Long id) {
        // 校验存在
        validateTaskExists(id);
        // 删除
        taskMapper.deleteById(id);
    }

    @Override
        public void deleteTaskListByIds(List<Long> ids) {
        // 删除
        taskMapper.deleteByIds(ids);
        }


    private void validateTaskExists(Long id) {
        if (taskMapper.selectById(id) == null) {
            throw exception(TASK_NOT_EXISTS);
        }
    }

    @Override
    public TaskDO getTask(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public PageResult<TaskDO> getTaskPage(TaskPageReqVO pageReqVO) {
        return taskMapper.selectPage(pageReqVO);
    }

}