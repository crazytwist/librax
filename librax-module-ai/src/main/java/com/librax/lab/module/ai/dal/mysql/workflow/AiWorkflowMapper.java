package com.librax.lab.module.ai.dal.mysql.workflow;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.ai.controller.admin.workflow.vo.AiWorkflowPageReqVO;
import com.librax.lab.module.ai.dal.dataobject.workflow.AiWorkflowDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 工作流 Mapper
 *
 * @author lesan
 */
@Mapper
public interface AiWorkflowMapper extends BaseMapperX<AiWorkflowDO> {

    default AiWorkflowDO selectByCode(String code) {
        return selectOne(AiWorkflowDO::getCode, code);
    }

    default PageResult<AiWorkflowDO> selectPage(AiWorkflowPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<AiWorkflowDO>()
                .eqIfPresent(AiWorkflowDO::getStatus, pageReqVO.getStatus())
                .likeIfPresent(AiWorkflowDO::getName, pageReqVO.getName())
                .likeIfPresent(AiWorkflowDO::getCode, pageReqVO.getCode())
                .betweenIfPresent(AiWorkflowDO::getCreateTime, pageReqVO.getCreateTime()));
    }

}
