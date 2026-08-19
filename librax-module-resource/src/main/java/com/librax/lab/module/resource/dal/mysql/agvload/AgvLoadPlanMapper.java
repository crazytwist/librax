package com.librax.lab.module.resource.dal.mysql.agvload;

import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadPlanDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgvLoadPlanMapper extends BaseMapperX<AgvLoadPlanDO> {

    default AgvLoadPlanDO selectByTaskId(String taskId) {
        return selectOne(new LambdaQueryWrapperX<AgvLoadPlanDO>()
                .eq(AgvLoadPlanDO::getTaskId, taskId));
    }

    /** 所有未终态（非COMPLETED/FAILED）的补料计划，供充电巡检/仓储窗口兜底轮询使用。 */
    default List<AgvLoadPlanDO> selectActive() {
        return selectList(new LambdaQueryWrapperX<AgvLoadPlanDO>()
                .notIn(AgvLoadPlanDO::getStatus, "COMPLETED", "FAILED"));
    }

    /** 该设备当前是否存在未终态的补料计划——充电巡检借此判断AGV是否真正空闲。 */
    default boolean hasActivePlan(String deviceId) {
        return selectCount(new LambdaQueryWrapperX<AgvLoadPlanDO>()
                .eq(AgvLoadPlanDO::getDeviceId, deviceId)
                .notIn(AgvLoadPlanDO::getStatus, "COMPLETED", "FAILED")) > 0;
    }

    /** 该设备当前所有未终态的补料计划，管理端状态查询/人工恢复用。 */
    default List<AgvLoadPlanDO> selectActiveByDeviceId(String deviceId) {
        return selectList(new LambdaQueryWrapperX<AgvLoadPlanDO>()
                .eq(AgvLoadPlanDO::getDeviceId, deviceId)
                .notIn(AgvLoadPlanDO::getStatus, "COMPLETED", "FAILED"));
    }

    @Select("SELECT * FROM lab_agv_load_plan WHERE task_id = #{taskId} AND deleted = 0 FOR UPDATE")
    AgvLoadPlanDO selectByTaskIdForUpdate(@Param("taskId") String taskId);

    default AgvLoadPlanDO selectByCurrentAgvTaskId(String agvTaskId) {
        return selectOne(new LambdaQueryWrapperX<AgvLoadPlanDO>()
                .eq(AgvLoadPlanDO::getCurrentAgvTaskId, agvTaskId));
    }

    @Select("SELECT * FROM lab_agv_load_plan WHERE current_agv_task_id = #{agvTaskId} AND deleted = 0 FOR UPDATE")
    AgvLoadPlanDO selectByCurrentAgvTaskIdForUpdate(@Param("agvTaskId") String agvTaskId);
}
