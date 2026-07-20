package com.librax.lab.module.resource.dal.mysql.agvload;

import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadPlanDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgvLoadPlanMapper extends BaseMapperX<AgvLoadPlanDO> {

    default AgvLoadPlanDO selectByTaskId(String taskId) {
        return selectOne(new LambdaQueryWrapperX<AgvLoadPlanDO>()
                .eq(AgvLoadPlanDO::getTaskId, taskId));
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
