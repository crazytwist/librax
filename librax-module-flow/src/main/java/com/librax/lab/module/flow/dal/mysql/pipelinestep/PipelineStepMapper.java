package com.librax.lab.module.flow.dal.mysql.pipelinestep;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.pipelinestep.PipelineStepDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.pipelinestep.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface PipelineStepMapper extends BaseMapperX<PipelineStepDO> {

    /**
     * 查询某流程版本下所有节点，按 sort_order 升序
     */
    List<PipelineStepDO> selectListByPipelineVersion(@Param("pipelineKey") String pipelineKey,
                                                     @Param("version") Integer version);

    default PageResult<PipelineStepDO> selectPage(PipelineStepPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PipelineStepDO>()
                .likeIfPresent(PipelineStepDO::getPipelineKey, reqVO.getPipelineKey())
                .eqIfPresent(PipelineStepDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(PipelineStepDO::getStepKey, reqVO.getStepKey())
                .betweenIfPresent(PipelineStepDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PipelineStepDO::getId));
    }

}