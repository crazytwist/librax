package com.librax.lab.module.flow.dal.mysql.definition;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.definition.FlowDefinitionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.definition.vo.*;

/**
 * 流程定义 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface DefinitionMapper extends BaseMapperX<FlowDefinitionDO> {

    default PageResult<FlowDefinitionDO> selectPage(DefinitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FlowDefinitionDO>()
                .eqIfPresent(FlowDefinitionDO::getFlowId, reqVO.getFlowId())
                .likeIfPresent(FlowDefinitionDO::getFlowName, reqVO.getFlowName())
                .eqIfPresent(FlowDefinitionDO::getFlowDesc, reqVO.getFlowDesc())
                .eqIfPresent(FlowDefinitionDO::getStartNodeId, reqVO.getStartNodeId())
                .eqIfPresent(FlowDefinitionDO::getFlowStatus, reqVO.getFlowStatus())
                .eqIfPresent(FlowDefinitionDO::getNodeIds, reqVO.getNodeIds())
                .eqIfPresent(FlowDefinitionDO::getFlowRules, reqVO.getFlowRules())
                .eqIfPresent(FlowDefinitionDO::getExt1, reqVO.getExt1())
                .eqIfPresent(FlowDefinitionDO::getExt2, reqVO.getExt2())
                .eqIfPresent(FlowDefinitionDO::getExtJson1, reqVO.getExtJson1())
                .eqIfPresent(FlowDefinitionDO::getExtJson2, reqVO.getExtJson2())
                .betweenIfPresent(FlowDefinitionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(FlowDefinitionDO::getId));
    }

}