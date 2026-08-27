package com.librax.lab.module.resource.dal.mysql.agvload;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvTaskQueueDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgvTaskQueueMapper extends BaseMapperX<AgvTaskQueueDO> {

    /** AGV 是否有正在执行中的任务（DISPATCHED 状态）。 */
    default boolean hasDispatched(String deviceId) {
        return selectCount(new LambdaQueryWrapperX<AgvTaskQueueDO>()
                .eq(AgvTaskQueueDO::getDeviceId, deviceId)
                .eq(AgvTaskQueueDO::getStatus, "DISPATCHED")) > 0;
    }

    /** 是否还有排队等待的任务（PENDING 状态），跟 hasDispatched 一起判断AGV是否真正空闲。 */
    default boolean hasPending(String deviceId) {
        return selectCount(new LambdaQueryWrapperX<AgvTaskQueueDO>()
                .eq(AgvTaskQueueDO::getDeviceId, deviceId)
                .eq(AgvTaskQueueDO::getStatus, "PENDING")) > 0;
    }

    /** 历史上出现过的所有 AGV 设备ID（去重），供空闲巡检定时任务遍历用。 */
    @Select("SELECT DISTINCT device_id FROM lab_agv_task_queue")
    List<String> selectDistinctDeviceIds();

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

    /**
     * 清空该设备所有未完成（PENDING/DISPATCHED）的队列项——人工恢复卡死设备用
     * （管理端 AgvOpsController "清空队列"接口）。不区分具体属于哪个计划，一次性全清，
     * 用于设备彻底卡死、单独按 agvTaskId 清不干净的场景。
     */
    default void cancelAllByDeviceId(String deviceId) {
        update(null, new LambdaUpdateWrapper<AgvTaskQueueDO>()
                .eq(AgvTaskQueueDO::getDeviceId, deviceId)
                .in(AgvTaskQueueDO::getStatus, "PENDING", "DISPATCHED")
                .set(AgvTaskQueueDO::getStatus, "CANCELLED"));
    }

    /** 该设备当前全部队列项（不限状态，按创建时间排序），管理端状态查询用。 */
    default List<AgvTaskQueueDO> selectAllByDeviceId(String deviceId) {
        return selectList(new LambdaQueryWrapperX<AgvTaskQueueDO>()
                .eq(AgvTaskQueueDO::getDeviceId, deviceId)
                .orderByAsc(AgvTaskQueueDO::getId));
    }
}
