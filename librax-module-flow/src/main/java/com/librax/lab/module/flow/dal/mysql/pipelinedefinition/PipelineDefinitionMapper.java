package com.librax.lab.module.flow.dal.mysql.pipelinedefinition;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.pipelinedefinition.PipelineDefinitionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.pipelinedefinition.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 流程定义表 Mapper
 *
 * @author 一南
 */
@Mapper
public interface PipelineDefinitionMapper extends BaseMapperX<PipelineDefinitionDO> {

    // ================================================================
    // 原有方法（不动）
    // ================================================================

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

    // ================================================================
    // ★ 新增：样本模式相关（SampleLifecycleService 使用）
    // ================================================================

    /**
     * 读取 sample_mode
     * 返回 "NONE" / "OPTIONAL" / "REQUIRED"，找不到默认 "REQUIRED"
     */
    default String selectSampleMode(String pipelineKey, Integer version) {
        PipelineDefinitionDO def = selectOne(
                new LambdaQueryWrapper<PipelineDefinitionDO>()
                        .select(PipelineDefinitionDO::getSampleMode)
                        .eq(PipelineDefinitionDO::getPipelineKey, pipelineKey)
                        .eq(PipelineDefinitionDO::getVersion, version));
        return def != null && def.getSampleMode() != null
                ? def.getSampleMode() : "REQUIRED";
    }

    /**
     * 读取 sample_bind_nodes（直接返回 List<String>，JacksonTypeHandler 自动反序列化）
     * null = 启动时立即绑定
     */
    default List<String> selectSampleBindNodes(String pipelineKey, Integer version) {
        PipelineDefinitionDO def = selectOne(
                new LambdaQueryWrapper<PipelineDefinitionDO>()
                        .select(PipelineDefinitionDO::getSampleBindNodes)
                        .eq(PipelineDefinitionDO::getPipelineKey, pipelineKey)
                        .eq(PipelineDefinitionDO::getVersion, version));
        return def != null ? def.getSampleBindNodes() : null;
    }
}