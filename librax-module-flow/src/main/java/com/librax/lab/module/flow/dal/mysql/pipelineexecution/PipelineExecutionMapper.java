package com.librax.lab.module.flow.dal.mysql.pipelineexecution;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.pipelineexecution.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 流程执行实例 Mapper
 *
 * @author 一南
 */
@Mapper
public interface PipelineExecutionMapper extends BaseMapperX<PipelineExecutionDO> {

    // ================================================================
    // 原有方法（不动）
    // ================================================================

    PipelineExecutionDO selectByExecutionId(@Param("executionId") String executionId);

    int compareAndSetStatus(@Param("executionId") String executionId,
                            @Param("fromStatus") String fromStatus,
                            @Param("toStatus") String toStatus);

    List<PipelineExecutionDO> selectAllRunning();

    default List<PipelineExecutionDO> selectByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<PipelineExecutionDO>()
                .eq(PipelineExecutionDO::getStatus, status)
                .eq(PipelineExecutionDO::getDeleted, false));
    }

    default PageResult<PipelineExecutionDO> selectPage(PipelineExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PipelineExecutionDO>()
                .eqIfPresent(PipelineExecutionDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(PipelineExecutionDO::getPipelineKey, reqVO.getPipelineKey())
                .eqIfPresent(PipelineExecutionDO::getPipelineVersion, reqVO.getPipelineVersion())
                .eqIfPresent(PipelineExecutionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PipelineExecutionDO::getTriggerType, reqVO.getTriggerType())
                .eqIfPresent(PipelineExecutionDO::getTriggeredBy, reqVO.getTriggeredBy())
                .eqIfPresent(PipelineExecutionDO::getInputParams, reqVO.getInputParams())
                .eqIfPresent(PipelineExecutionDO::getParentExecutionId, reqVO.getParentExecutionId())
                .eqIfPresent(PipelineExecutionDO::getStandaloneNodeId, reqVO.getStandaloneNodeId())
                .eqIfPresent(PipelineExecutionDO::getOriginExecutionId, reqVO.getOriginExecutionId())
                .eqIfPresent(PipelineExecutionDO::getStartedAt, reqVO.getStartedAt())
                .eqIfPresent(PipelineExecutionDO::getFinishedAt, reqVO.getFinishedAt())
                .eqIfPresent(PipelineExecutionDO::getTotalMs, reqVO.getTotalMs())
                .eqIfPresent(PipelineExecutionDO::getCriticalPath, reqVO.getCriticalPath())
                .eqIfPresent(PipelineExecutionDO::getRowVersion, reqVO.getRowVersion())
                .betweenIfPresent(PipelineExecutionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PipelineExecutionDO::getId));
    }

    // ================================================================
    // ★ 新增：样本模式相关（SampleLifecycleService 使用）
    // ================================================================

    /**
     * 读取 sample_mode
     * 返回 "NONE" / "OPTIONAL" / "REQUIRED"，找不到默认 "REQUIRED"
     */
    default String selectSampleMode(String executionId) {
        PipelineExecutionDO exec = selectOne(
                new LambdaQueryWrapperX<PipelineExecutionDO>()
                        .select(PipelineExecutionDO::getSampleMode)
                        .eq(PipelineExecutionDO::getExecutionId, executionId));
        return exec != null && exec.getSampleMode() != null
                ? exec.getSampleMode() : "REQUIRED";
    }

    /**
     * 读取 sample_bind_nodes（直接返回 List<String>，JacksonTypeHandler 自动反序列化）
     * null = 启动时立即绑定
     */
    default List<String> selectSampleBindNodes(String executionId) {
        PipelineExecutionDO exec = selectOne(
                new LambdaQueryWrapperX<PipelineExecutionDO>()
                        .select(PipelineExecutionDO::getSampleBindNodes)
                        .eq(PipelineExecutionDO::getExecutionId, executionId));
        return exec != null ? exec.getSampleBindNodes() : null;
    }

    /**
     * 读取待消费的样本 ID 队列（逗号分隔，FIFO）
     */
    default String selectPendingSampleIds(String executionId) {
        PipelineExecutionDO exec = selectOne(
                new LambdaQueryWrapperX<PipelineExecutionDO>()
                        .select(PipelineExecutionDO::getPendingSampleIds)
                        .eq(PipelineExecutionDO::getExecutionId, executionId));
        return exec != null ? exec.getPendingSampleIds() : null;
    }

    /**
     * 更新待消费样本 ID 队列
     * 每次延迟绑定消费一个后，把剩余队列写回
     * 全部消费完时传入空字符串 ""
     */
    default void updatePendingSampleIds(String executionId, String pendingSampleIds) {
        update(null, new LambdaUpdateWrapper<PipelineExecutionDO>()
                .set(PipelineExecutionDO::getPendingSampleIds, pendingSampleIds)
                .eq(PipelineExecutionDO::getExecutionId, executionId));
    }

    /**
     * 读取 pipeline_key
     */
    default String selectPipelineKey(String executionId) {
        PipelineExecutionDO exec = selectOne(
                new LambdaQueryWrapperX<PipelineExecutionDO>()
                        .select(PipelineExecutionDO::getPipelineKey)
                        .eq(PipelineExecutionDO::getExecutionId, executionId));
        return exec != null ? exec.getPipelineKey() : null;
    }

    /**
     * 读取 pipeline_version
     */
    default Integer selectPipelineVersion(String executionId) {
        PipelineExecutionDO exec = selectOne(
                new LambdaQueryWrapperX<PipelineExecutionDO>()
                        .select(PipelineExecutionDO::getPipelineVersion)
                        .eq(PipelineExecutionDO::getExecutionId, executionId));
        return exec != null ? exec.getPipelineVersion() : null;
    }
}