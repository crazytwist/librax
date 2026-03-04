package com.librax.lab.module.flow.dal.mysql.nodedefinition;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.nodedefinition.NodeDefinitionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.nodedefinition.vo.*;

/**
 * 流程节点定义 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface NodeDefinitionMapper extends BaseMapperX<NodeDefinitionDO> {

    default PageResult<NodeDefinitionDO> selectPage(NodeDefinitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<NodeDefinitionDO>()
                .eqIfPresent(NodeDefinitionDO::getNodeId, reqVO.getNodeId())
                .likeIfPresent(NodeDefinitionDO::getNodeName, reqVO.getNodeName())
                .eqIfPresent(NodeDefinitionDO::getNodeCode, reqVO.getNodeCode())
                .eqIfPresent(NodeDefinitionDO::getNodeType, reqVO.getNodeType())
                .eqIfPresent(NodeDefinitionDO::getNodeCategory, reqVO.getNodeCategory())
                .eqIfPresent(NodeDefinitionDO::getNodeDesc, reqVO.getNodeDesc())
                .eqIfPresent(NodeDefinitionDO::getNodeStatus, reqVO.getNodeStatus())
                .eqIfPresent(NodeDefinitionDO::getExecuteConfig, reqVO.getExecuteConfig())
                .eqIfPresent(NodeDefinitionDO::getNodeParams, reqVO.getNodeParams())
                .eqIfPresent(NodeDefinitionDO::getExt1, reqVO.getExt1())
                .eqIfPresent(NodeDefinitionDO::getExt2, reqVO.getExt2())
                .eqIfPresent(NodeDefinitionDO::getExtJson1, reqVO.getExtJson1())
                .eqIfPresent(NodeDefinitionDO::getExtJson2, reqVO.getExtJson2())
                .betweenIfPresent(NodeDefinitionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(NodeDefinitionDO::getId));
    }

}