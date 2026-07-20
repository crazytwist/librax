package com.librax.lab.module.resource.dal.mysql.agvload;

import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnPlanDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgvReturnPlanMapper extends BaseMapperX<AgvReturnPlanDO> {

    default AgvReturnPlanDO selectByTaskId(String taskId) {
        return selectOne(new LambdaQueryWrapperX<AgvReturnPlanDO>()
                .eq(AgvReturnPlanDO::getTaskId, taskId));
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
