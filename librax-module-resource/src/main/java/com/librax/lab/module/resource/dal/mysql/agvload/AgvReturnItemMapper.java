package com.librax.lab.module.resource.dal.mysql.agvload;

import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgvReturnItemMapper extends BaseMapperX<AgvReturnItemDO> {

    default List<AgvReturnItemDO> selectByTaskId(String taskId) {
        return selectList(new LambdaQueryWrapperX<AgvReturnItemDO>()
                .eq(AgvReturnItemDO::getTaskId, taskId)
                .orderByAsc(AgvReturnItemDO::getSequenceNo));
    }

    default List<AgvReturnItemDO> selectByTaskIdAndWave(String taskId, int waveNo) {
        return selectList(new LambdaQueryWrapperX<AgvReturnItemDO>()
                .eq(AgvReturnItemDO::getTaskId, taskId)
                .eq(AgvReturnItemDO::getWaveNo, waveNo)
                .orderByAsc(AgvReturnItemDO::getSequenceNo));
    }

    default List<AgvReturnItemDO> selectByTaskIdAndStatus(String taskId, String status) {
        return selectList(new LambdaQueryWrapperX<AgvReturnItemDO>()
                .eq(AgvReturnItemDO::getTaskId, taskId)
                .eq(AgvReturnItemDO::getStatus, status)
                .orderByAsc(AgvReturnItemDO::getSequenceNo));
    }

    /** 取当前波次中下一个等待仓储入库（IN_TRANSIT，尚未发出请求）的物料。 */
    default AgvReturnItemDO selectNextPendingReturn(String taskId, int waveNo) {
        return selectOne(new LambdaQueryWrapperX<AgvReturnItemDO>()
                .eq(AgvReturnItemDO::getTaskId, taskId)
                .eq(AgvReturnItemDO::getWaveNo, waveNo)
                .eq(AgvReturnItemDO::getStatus, "IN_TRANSIT")
                .isNull(AgvReturnItemDO::getWarehouseRequestId)
                .orderByAsc(AgvReturnItemDO::getSequenceNo)
                .last("LIMIT 1"));
    }

    default AgvReturnItemDO selectByTransitSlot(String taskId, String transitSlotId) {
        return selectOne(new LambdaQueryWrapperX<AgvReturnItemDO>()
                .eq(AgvReturnItemDO::getTaskId, taskId)
                .eq(AgvReturnItemDO::getTransitSlotId, transitSlotId)
                .eq(AgvReturnItemDO::getStatus, "IN_TRANSIT")
                .last("LIMIT 1"));
    }
}
