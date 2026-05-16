package com.librax.lab.module.resource.dal.mysql.stepresourcehold;

import java.time.LocalDateTime;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.resource.dal.dataobject.stepresourcehold.StepResourceHoldDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.resource.controller.admin.stepresourcehold.vo.*;

/**
 * 步骤执行资源占用记录 Mapper
 *
 * @author 一南
 */
@Mapper
public interface StepResourceHoldMapper extends BaseMapperX<StepResourceHoldDO> {

    default PageResult<StepResourceHoldDO> selectPage(StepResourceHoldPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StepResourceHoldDO>()
                .orderByDesc(StepResourceHoldDO::getId));
    }

    /**
     * 查步骤当前持有中的资源（released_at IS NULL）
     * 用于：
     *   1. ResourcePoolImpl.release() 判断是否 INHERITED，决定是否跳过释放
     *   2. ResourcePoolImpl.queryHeldResourceId() 查当前持有的 resourceId
     */
    default StepResourceHoldDO selectActiveHold(String executionId,
                                                String nodeId,
                                                int attempt) {
        return selectOne(new LambdaQueryWrapper<StepResourceHoldDO>()
                .eq(StepResourceHoldDO::getExecutionId, executionId)
                .eq(StepResourceHoldDO::getNodeId, nodeId)
                .eq(StepResourceHoldDO::getAttempt, attempt)
                .isNull(StepResourceHoldDO::getReleasedAt));
    }

    /**
     * 标记资源持有记录为已释放
     */
    default int markReleased(String executionId, String nodeId,
                             int attempt, String reason, LocalDateTime releasedAt) {
        return update(null, new LambdaUpdateWrapper<StepResourceHoldDO>()
                .eq(StepResourceHoldDO::getExecutionId, executionId)
                .eq(StepResourceHoldDO::getNodeId, nodeId)
                .eq(StepResourceHoldDO::getAttempt, attempt)
                .isNull(StepResourceHoldDO::getReleasedAt)
                .set(StepResourceHoldDO::getReleasedAt, releasedAt)
                .set(StepResourceHoldDO::getReleaseReason, reason));
    }

    /**
     * tryStart 失败时回滚：物理删除刚写入的 hold 记录
     */
    default int deleteByAttempt(String executionId, String nodeId, int attempt) {
        return delete(new LambdaQueryWrapper<StepResourceHoldDO>()
                .eq(StepResourceHoldDO::getExecutionId, executionId)
                .eq(StepResourceHoldDO::getNodeId, nodeId)
                .eq(StepResourceHoldDO::getAttempt, attempt));
    }

    /**
     * 查指定执行ID下，同类型的活跃 ACQUIRED hold 记录
     *
     * 用于 ResourcePoolImpl.tryInheritFromParent：
     *   子节点申请资源时，查父执行链是否已有同类型的 ACQUIRED 记录可继承
     *
     * 条件：
     *   execution_id  = parentExecutionId
     *   resource_type = resourceType
     *   released_at   IS NULL（活跃中）
     *   hold_type     = 'ACQUIRED'（只找自己申请的，防止继承链循环）
     *
     * 注意：表里没有 zone_code 列，不做 zone 过滤
     *   同一个执行ID下同类型只会预占一台设备，不会误匹配
     */
    default StepResourceHoldDO selectActiveHoldByExecutionAndType(
            String executionId, String resourceType) {
        return selectOne(new LambdaQueryWrapper<StepResourceHoldDO>()
                .eq(StepResourceHoldDO::getExecutionId, executionId)
                .eq(StepResourceHoldDO::getResourceType, resourceType)
                .isNull(StepResourceHoldDO::getReleasedAt)
                .eq(StepResourceHoldDO::getHoldType, "ACQUIRED")
                .last("LIMIT 1"));
    }
}