package com.librax.lab.module.resource.dal.mysql.agvload;

import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnPlanDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgvReturnPlanMapper extends BaseMapperX<AgvReturnPlanDO> {

    default AgvReturnPlanDO selectByTaskId(String taskId) {
        return selectOne(new LambdaQueryWrapperX<AgvReturnPlanDO>()
                .eq(AgvReturnPlanDO::getTaskId, taskId));
    }

    /** 该设备当前是否存在未终态的下料计划——充电巡检借此判断AGV是否真正空闲。 */
    default boolean hasActivePlan(String deviceId) {
        return selectCount(new LambdaQueryWrapperX<AgvReturnPlanDO>()
                .eq(AgvReturnPlanDO::getDeviceId, deviceId)
                .notIn(AgvReturnPlanDO::getStatus, "COMPLETED", "FAILED")) > 0;
    }

    /** 该设备当前所有未终态的下料计划，管理端状态查询/人工恢复用。 */
    default List<AgvReturnPlanDO> selectActiveByDeviceId(String deviceId) {
        return selectList(new LambdaQueryWrapperX<AgvReturnPlanDO>()
                .eq(AgvReturnPlanDO::getDeviceId, deviceId)
                .notIn(AgvReturnPlanDO::getStatus, "COMPLETED", "FAILED"));
    }

    /** 所有未终态（非COMPLETED/FAILED）的下料计划，供仓储窗口兜底轮询使用。 */
    default List<AgvReturnPlanDO> selectActive() {
        return selectList(new LambdaQueryWrapperX<AgvReturnPlanDO>()
                .notIn(AgvReturnPlanDO::getStatus, "COMPLETED", "FAILED"));
    }

    @Select("SELECT * FROM lab_agv_return_plan WHERE task_id = #{taskId} AND deleted = 0 FOR UPDATE")
    AgvReturnPlanDO selectByTaskIdForUpdate(@Param("taskId") String taskId);

    default AgvReturnPlanDO selectByCurrentAgvTaskId(String agvTaskId) {
        return selectOne(new LambdaQueryWrapperX<AgvReturnPlanDO>()
                .eq(AgvReturnPlanDO::getCurrentAgvTaskId, agvTaskId));
    }

    @Select("SELECT * FROM lab_agv_return_plan WHERE current_agv_task_id = #{agvTaskId} AND deleted = 0 FOR UPDATE")
    AgvReturnPlanDO selectByCurrentAgvTaskIdForUpdate(@Param("agvTaskId") String agvTaskId);
}
