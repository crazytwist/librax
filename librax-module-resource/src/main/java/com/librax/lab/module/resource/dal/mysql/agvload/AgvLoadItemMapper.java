package com.librax.lab.module.resource.dal.mysql.agvload;

import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgvLoadItemMapper extends BaseMapperX<AgvLoadItemDO> {

    default List<AgvLoadItemDO> selectByTaskId(String taskId) {
        return selectList(new LambdaQueryWrapperX<AgvLoadItemDO>()
                .eq(AgvLoadItemDO::getTaskId, taskId)
                .orderByAsc(AgvLoadItemDO::getSequenceNo));
    }

    default List<AgvLoadItemDO> selectByTaskIdAndStatus(String taskId, String status) {
        return selectList(new LambdaQueryWrapperX<AgvLoadItemDO>()
                .eq(AgvLoadItemDO::getTaskId, taskId)
                .eq(AgvLoadItemDO::getStatus, status)
                .orderByAsc(AgvLoadItemDO::getSequenceNo));
    }

    default AgvLoadItemDO selectNextWaiting(String taskId, String sourceSlotId) {
        return selectOne(new LambdaQueryWrapperX<AgvLoadItemDO>()
                .eq(AgvLoadItemDO::getTaskId, taskId)
                .eq(AgvLoadItemDO::getSourceSlotId, sourceSlotId)
                .eq(AgvLoadItemDO::getStatus, "WAIT_SOURCE")
                .orderByAsc(AgvLoadItemDO::getSequenceNo)
                .last("LIMIT 1"));
    }

    default AgvLoadItemDO selectByReadyRequestId(String requestId) {
        return selectOne(new LambdaQueryWrapperX<AgvLoadItemDO>()
                .eq(AgvLoadItemDO::getReadyRequestId, requestId));
    }

    default AgvLoadItemDO selectByWarehouseRequestId(String requestId) {
        return selectOne(new LambdaQueryWrapperX<AgvLoadItemDO>()
                .eq(AgvLoadItemDO::getWarehouseRequestId, requestId));
    }

    default AgvLoadItemDO selectByTaskIdAndSourceSlot(String taskId, String sourceSlotId) {
        return selectOne(new LambdaQueryWrapperX<AgvLoadItemDO>()
                .eq(AgvLoadItemDO::getTaskId, taskId)
                .eq(AgvLoadItemDO::getSourceSlotId, sourceSlotId)
                .last("LIMIT 1"));
    }
}
