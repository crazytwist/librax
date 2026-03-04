package com.librax.lab.module.flow.service.definition;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import com.librax.lab.module.flow.controller.admin.definition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.definition.FlowDefinitionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.definition.DefinitionMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.FLOW_DEFINITION_NOT_EXISTS;

/**
 * 流程定义 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class DefinitionServiceImpl implements DefinitionService {

    @Resource
    private DefinitionMapper definitionMapper;

    @Override
    public Long createDefinition(DefinitionSaveReqVO createReqVO) {
        // 插入
        FlowDefinitionDO definition = BeanUtils.toBean(createReqVO, FlowDefinitionDO.class);
        definitionMapper.insert(definition);

        // 返回
        return definition.getId();
    }

    @Override
    public void updateDefinition(DefinitionSaveReqVO updateReqVO) {
        // 校验存在
        validateDefinitionExists(updateReqVO.getId());
        // 更新
        FlowDefinitionDO updateObj = BeanUtils.toBean(updateReqVO, FlowDefinitionDO.class);
        definitionMapper.updateById(updateObj);
    }

    @Override
    public void deleteDefinition(Long id) {
        // 校验存在
        validateDefinitionExists(id);
        // 删除
        definitionMapper.deleteById(id);
    }

    @Override
        public void deleteDefinitionListByIds(List<Long> ids) {
        // 删除
        definitionMapper.deleteByIds(ids);
        }


    private void validateDefinitionExists(Long id) {
        if (definitionMapper.selectById(id) == null) {
            throw exception(FLOW_DEFINITION_NOT_EXISTS);
        }
    }

    @Override
    public FlowDefinitionDO getDefinition(Long id) {
        return definitionMapper.selectById(id);
    }

    @Override
    public PageResult<FlowDefinitionDO> getDefinitionPage(DefinitionPageReqVO pageReqVO) {
        return definitionMapper.selectPage(pageReqVO);
    }

}