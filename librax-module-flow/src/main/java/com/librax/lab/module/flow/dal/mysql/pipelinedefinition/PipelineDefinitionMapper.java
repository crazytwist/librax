package com.librax.lab.module.flow.dal.mysql.pipelinedefinition;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.pipelinedefinition.PipelineDefinitionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.pipelinedefinition.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_] Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface PipelineDefinitionMapper extends BaseMapperX<PipelineDefinitionDO> {


    /**
     * 按 pipeline_key + version 查询（filtered: deleted = 0）
     */
    PipelineDefinitionDO selectByKeyAndVersion(@Param("pipelineKey") String pipelineKey,
                                               @Param("version") Integer version);

    /**
     * 查询某 key 下最新的 ACTIVE 版本
     */
    PipelineDefinitionDO selectLatestActive(@Param("pipelineKey") String pipelineKey);


    default PageResult<PipelineDefinitionDO> selectPage(PipelineDefinitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PipelineDefinitionDO>()
                .eqIfPresent(PipelineDefinitionDO::getPipelineKey, reqVO.getPipelineKey())
                .eqIfPresent(PipelineDefinitionDO::getVersion, reqVO.getVersion())
                .likeIfPresent(PipelineDefinitionDO::getName, reqVO.getName())
                .eqIfPresent(PipelineDefinitionDO::getDescription, reqVO.getDescription())
                .eqIfPresent(PipelineDefinitionDO::getFailStrategy, reqVO.getFailStrategy())
                .eqIfPresent(PipelineDefinitionDO::getCompensateStrategy, reqVO.getCompensateStrategy())
                .eqIfPresent(PipelineDefinitionDO::getDefaultTimeoutMs, reqVO.getDefaultTimeoutMs())
                .eqIfPresent(PipelineDefinitionDO::getDefaultMaxAttempts, reqVO.getDefaultMaxAttempts())
                .eqIfPresent(PipelineDefinitionDO::getDefaultBackoffMs, reqVO.getDefaultBackoffMs())
                .eqIfPresent(PipelineDefinitionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PipelineDefinitionDO::getPublishedAt, reqVO.getPublishedAt())
                .betweenIfPresent(PipelineDefinitionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PipelineDefinitionDO::getId));
    }

}