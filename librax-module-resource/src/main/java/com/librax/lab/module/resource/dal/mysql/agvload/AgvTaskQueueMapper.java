package com.librax.lab.module.resource.dal.mysql.agvload;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvTaskQueueDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgvTaskQueueMapper extends BaseMapperX<AgvTaskQueueDO> {

    /** AGV 是否有正在执行中的任务（DISPATCHED 状态）。 */
    default boolean hasDispatched(String deviceId) {
        return selectCount(new LambdaQueryWrapperX<AgvTaskQueueDO>()
                .eq(AgvTaskQueueDO::getDeviceId, deviceId)
                .eq(AgvTaskQueueDO::getStatus, "DISPATCHED")) > 0;
    }

    /** 取队首待调度任务（最早入队的 PENDING 项）。 */
    default AgvTaskQueueDO selectNextPending(String deviceId) {
        return selectOne(new LambdaQueryWrapperX<AgvTaskQueueDO>()
                .eq(AgvTaskQueueDO::getDeviceId, deviceId)
                .eq(AgvTaskQueueDO::getStatus, "PENDING")
                .orderByAsc(AgvTaskQueueDO::getId)
                .last("LIMIT 1"));
    }

    /** AGV 任务完成，将对应队列项标记为 DONE。 */
    default void markDoneByAgvTaskId(String agvTaskId) {
        update(null, new LambdaUpdateWrapper<AgvTaskQueueDO>()
                .eq(AgvTaskQueueDO::getAgvTaskId, agvTaskId)
                .eq(AgvTaskQueueDO::getStatus, "DISPATCHED")
                .set(AgvTaskQueueDO::getStatus, "DONE"));
    }

    /** AGV 任务失败或计划取消，将队列项标记为 CANCELLED。 */
    default void cancelByAgvTaskId(String agvTaskId) {
        update(null, new LambdaUpdateWrapper<AgvTaskQueueDO>()
                .eq(AgvTaskQueueDO::getAgvTaskId, agvTaskId)
                .in(AgvTaskQueueDO::getStatus, "PENDING", "DISPATCHED")
                .set(AgvTaskQueueDO::getStatus, "CANCELLED"));
    }
}
