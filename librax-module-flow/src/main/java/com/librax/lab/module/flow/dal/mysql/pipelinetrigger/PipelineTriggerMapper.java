package com.librax.lab.module.flow.dal.mysql.pipelinetrigger;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.pipelinetrigger.PipelineTriggerDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.pipelinetrigger.vo.*;

/**
 * 流程触发配置表，管理定时和事件触发规则 [pd_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface PipelineTriggerMapper extends BaseMapperX<PipelineTriggerDO> {

    default PageResult<PipelineTriggerDO> selectPage(PipelineTriggerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PipelineTriggerDO>()
                .eqIfPresent(PipelineTriggerDO::getTriggerId, reqVO.getTriggerId())
                .eqIfPresent(PipelineTriggerDO::getPipelineKey, reqVO.getPipelineKey())
                .eqIfPresent(PipelineTriggerDO::getTriggerType, reqVO.getTriggerType())
                .eqIfPresent(PipelineTriggerDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PipelineTriggerDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PipelineTriggerDO::getId));
    }

}