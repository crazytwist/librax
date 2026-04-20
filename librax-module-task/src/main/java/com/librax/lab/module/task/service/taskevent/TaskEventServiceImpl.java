package com.librax.lab.module.task.service.taskevent;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.task.controller.admin.taskevent.vo.*;
import com.librax.lab.module.task.dal.dataobject.taskevent.TaskEventDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.task.dal.mysql.taskevent.TaskEventMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.task.enums.ErrorCodeConstants.*;

/**
 * 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class TaskEventServiceImpl implements TaskEventService {

    @Resource
    private TaskEventMapper eventMapper;

    @Override
    public Long createEvent(TaskEventSaveReqVO createReqVO) {
        // 插入
        TaskEventDO event = BeanUtils.toBean(createReqVO, TaskEventDO.class);
        eventMapper.insert(event);

        // 返回
        return event.getId();
    }

    @Override
    public void updateEvent(TaskEventSaveReqVO updateReqVO) {
        // 校验存在
        validateEventExists(updateReqVO.getId());
        // 更新
        TaskEventDO updateObj = BeanUtils.toBean(updateReqVO, TaskEventDO.class);
        eventMapper.updateById(updateObj);
    }

    @Override
    public void deleteEvent(Long id) {
        // 校验存在
        validateEventExists(id);
        // 删除
        eventMapper.deleteById(id);
    }

    @Override
        public void deleteEventListByIds(List<Long> ids) {
        // 删除
        eventMapper.deleteByIds(ids);
        }


    private void validateEventExists(Long id) {
        if (eventMapper.selectById(id) == null) {
            throw exception(EVENT_NOT_EXISTS);
        }
    }

    @Override
    public TaskEventDO getEvent(Long id) {
        return eventMapper.selectById(id);
    }

    @Override
    public PageResult<TaskEventDO> getEventPage(TaskEventPageReqVO pageReqVO) {
        return eventMapper.selectPage(pageReqVO);
    }

}