package com.librax.lab.module.flow.service.pipelinedefinition;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import com.librax.lab.module.flow.controller.admin.pipelinedefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinedefinition.PipelineDefinitionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.pipelinedefinition.PipelineDefinitionMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_] Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class PipelineDefinitionServiceImpl implements PipelineDefinitionService {

    @Resource
    private PipelineDefinitionMapper pipelineDefinitionMapper;

    @Override
    public Long createPipelineDefinition(PipelineDefinitionSaveReqVO createReqVO) {
        // 插入
        PipelineDefinitionDO pipelineDefinition = BeanUtils.toBean(createReqVO, PipelineDefinitionDO.class);
        pipelineDefinitionMapper.insert(pipelineDefinition);

        // 返回
        return pipelineDefinition.getId();
    }

    @Override
    public void updatePipelineDefinition(PipelineDefinitionSaveReqVO updateReqVO) {
        // 校验存在
        validatePipelineDefinitionExists(updateReqVO.getId());
        // 更新
        PipelineDefinitionDO updateObj = BeanUtils.toBean(updateReqVO, PipelineDefinitionDO.class);
        pipelineDefinitionMapper.updateById(updateObj);
    }

    @Override
    public void deletePipelineDefinition(Long id) {
        // 校验存在
        validatePipelineDefinitionExists(id);
        // 删除
        pipelineDefinitionMapper.deleteById(id);
    }

    @Override
    public void deletePipelineDefinitionListByIds(List<Long> ids) {
        // 删除
        pipelineDefinitionMapper.deleteByIds(ids);
    }


    private void validatePipelineDefinitionExists(Long id) {
        if (pipelineDefinitionMapper.selectById(id) == null) {
            throw exception(PIPELINE_DEFINITION_NOT_EXISTS);
        }
    }

    @Override
    public PipelineDefinitionDO getPipelineDefinition(Long id) {
        return pipelineDefinitionMapper.selectById(id);
    }

    @Override
    public PageResult<PipelineDefinitionDO> getPipelineDefinitionPage(PipelineDefinitionPageReqVO pageReqVO) {
        return pipelineDefinitionMapper.selectPage(pageReqVO);
    }

}