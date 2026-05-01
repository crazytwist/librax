package com.librax.lab.module.resource.dal.mysql.stepresourcehold;

import java.time.LocalDateTime;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.resource.dal.dataobject.stepresourcehold.StepResourceHoldDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.resource.controller.admin.stepresourcehold.vo.*;

/**
 * 步骤执行资源占用记录，released_at IS NULL 表示当前持有中 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface StepResourceHoldMapper extends BaseMapperX<StepResourceHoldDO> {

    default PageResult<StepResourceHoldDO> selectPage(StepResourceHoldPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StepResourceHoldDO>()
                .orderByDesc(StepResourceHoldDO::getId));
    }

    /**
     * 查询步骤当前持有中的资源（released_at IS NULL）
     */
    default StepResourceHoldDO selectActiveHold(String executionId,
                                                String nodeId,
                                                int attempt) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StepResourceHoldDO>()
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
        return delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StepResourceHoldDO>()
                .eq(StepResourceHoldDO::getExecutionId, executionId)
                .eq(StepResourceHoldDO::getNodeId, nodeId)
                .eq(StepResourceHoldDO::getAttempt, attempt));
    }

}