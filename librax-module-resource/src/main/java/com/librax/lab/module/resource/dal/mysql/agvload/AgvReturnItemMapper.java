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

    /**
     * 取计划内最早一条已到中转位、尚未向仓储发请求的物料。
     * 先按波次再按序号，避免 currentWave 被提前卸料拨走后，更早波次的 IN_TRANSIT 再也选不中。
     */
    default AgvReturnItemDO selectEarliestPendingReturn(String taskId) {
        return selectOne(new LambdaQueryWrapperX<AgvReturnItemDO>()
                .eq(AgvReturnItemDO::getTaskId, taskId)
                .eq(AgvReturnItemDO::getStatus, "IN_TRANSIT")
                .isNull(AgvReturnItemDO::getWarehouseRequestId)
                .orderByAsc(AgvReturnItemDO::getWaveNo, AgvReturnItemDO::getSequenceNo)
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
