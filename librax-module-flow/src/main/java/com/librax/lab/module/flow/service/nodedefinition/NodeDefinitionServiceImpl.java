package com.librax.lab.module.flow.service.nodedefinition;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.flow.controller.admin.nodedefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.nodedefinition.NodeDefinitionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.nodedefinition.NodeDefinitionMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 流程节点定义 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class NodeDefinitionServiceImpl implements NodeDefinitionService {

    @Resource
    private NodeDefinitionMapper nodeDefinitionMapper;

    @Override
    public Long createNodeDefinition(NodeDefinitionSaveReqVO createReqVO) {
        // 插入
        NodeDefinitionDO nodeDefinition = BeanUtils.toBean(createReqVO, NodeDefinitionDO.class);
        nodeDefinitionMapper.insert(nodeDefinition);

        // 返回
        return nodeDefinition.getId();
    }

    @Override
    public void updateNodeDefinition(NodeDefinitionSaveReqVO updateReqVO) {
        // 校验存在
        validateNodeDefinitionExists(updateReqVO.getId());
        // 更新
        NodeDefinitionDO updateObj = BeanUtils.toBean(updateReqVO, NodeDefinitionDO.class);
        nodeDefinitionMapper.updateById(updateObj);
    }

    @Override
    public void deleteNodeDefinition(Long id) {
        // 校验存在
        validateNodeDefinitionExists(id);
        // 删除
        nodeDefinitionMapper.deleteById(id);
    }

    @Override
        public void deleteNodeDefinitionListByIds(List<Long> ids) {
        // 删除
        nodeDefinitionMapper.deleteByIds(ids);
        }


    private void validateNodeDefinitionExists(Long id) {
        if (nodeDefinitionMapper.selectById(id) == null) {
            throw exception(NODE_DEFINITION_NOT_EXISTS);
        }
    }

    @Override
    public NodeDefinitionDO getNodeDefinition(Long id) {
        return nodeDefinitionMapper.selectById(id);
    }

    @Override
    public PageResult<NodeDefinitionDO> getNodeDefinitionPage(NodeDefinitionPageReqVO pageReqVO) {
        return nodeDefinitionMapper.selectPage(pageReqVO);
    }

}