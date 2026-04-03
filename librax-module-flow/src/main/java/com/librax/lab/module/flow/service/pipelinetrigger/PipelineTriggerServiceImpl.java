package com.librax.lab.module.flow.service.pipelinetrigger;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.flow.controller.admin.pipelinetrigger.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinetrigger.PipelineTriggerDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.pipelinetrigger.PipelineTriggerMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 流程触发配置表，管理定时和事件触发规则 [pd_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class PipelineTriggerServiceImpl implements PipelineTriggerService {

    @Resource
    private PipelineTriggerMapper pipelineTriggerMapper;

    @Override
    public Long createPipelineTrigger(PipelineTriggerSaveReqVO createReqVO) {
        // 插入
        PipelineTriggerDO pipelineTrigger = BeanUtils.toBean(createReqVO, PipelineTriggerDO.class);
        pipelineTriggerMapper.insert(pipelineTrigger);

        // 返回
        return pipelineTrigger.getId();
    }

    @Override
    public void updatePipelineTrigger(PipelineTriggerSaveReqVO updateReqVO) {
        // 校验存在
        validatePipelineTriggerExists(updateReqVO.getId());
        // 更新
        PipelineTriggerDO updateObj = BeanUtils.toBean(updateReqVO, PipelineTriggerDO.class);
        pipelineTriggerMapper.updateById(updateObj);
    }

    @Override
    public void deletePipelineTrigger(Long id) {
        // 校验存在
        validatePipelineTriggerExists(id);
        // 删除
        pipelineTriggerMapper.deleteById(id);
    }

    @Override
        public void deletePipelineTriggerListByIds(List<Long> ids) {
        // 删除
        pipelineTriggerMapper.deleteByIds(ids);
        }


    private void validatePipelineTriggerExists(Long id) {
        if (pipelineTriggerMapper.selectById(id) == null) {
            throw exception(PIPELINE_TRIGGER_NOT_EXISTS);
        }
    }

    @Override
    public PipelineTriggerDO getPipelineTrigger(Long id) {
        return pipelineTriggerMapper.selectById(id);
    }

    @Override
    public PageResult<PipelineTriggerDO> getPipelineTriggerPage(PipelineTriggerPageReqVO pageReqVO) {
        return pipelineTriggerMapper.selectPage(pageReqVO);
    }

}