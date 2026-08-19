package com.librax.lab.module.resource.dal.mysql.agvload;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvOrchestrateQueueDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AgvOrchestrateQueueMapper extends BaseMapperX<AgvOrchestrateQueueDO> {

    /**
     * 巡检定时任务用：查所有还有 PENDING 排队记录的设备号，供轮询兜底重试遍历。
     * 事件唤醒（槽位释放时 afterCommit 触发）是主路径，这个只是兜底——防止某次唤醒因为
     * 异常/漏写/进程重启中间态而丢失，导致排队记录卡到超时都没人再看它。
     */
    @Select("SELECT DISTINCT device_id FROM lab_agv_orchestrate_queue WHERE status = 'PENDING'")
    List<String> selectDistinctPendingDeviceIds();

    /** 按 executionId 查排队记录（幂等键，一次流程执行只应该有一条）。 */
    default AgvOrchestrateQueueDO selectByExecutionId(String executionId) {
        return selectOne(new LambdaQueryWrapperX<AgvOrchestrateQueueDO>()
                .eq(AgvOrchestrateQueueDO::getExecutionId, executionId));
    }

    /** 取队首待重试记录（最早入队的 PENDING 项），按 deviceId 隔离，FIFO。 */
    default AgvOrchestrateQueueDO selectOldestPending(String deviceId) {
        return selectOne(new LambdaQueryWrapperX<AgvOrchestrateQueueDO>()
                .eq(AgvOrchestrateQueueDO::getDeviceId, deviceId)
                .eq(AgvOrchestrateQueueDO::getStatus, "PENDING")
                .orderByAsc(AgvOrchestrateQueueDO::getId)
                .last("LIMIT 1"));
    }

    /** 重试失败，只更新失败原因，状态保持 PENDING，留给下一次触发。 */
    default void updateRetryFailure(Long id, String errorMessage) {
        update(null, new LambdaUpdateWrapper<AgvOrchestrateQueueDO>()
                .eq(AgvOrchestrateQueueDO::getId, id)
                .eq(AgvOrchestrateQueueDO::getStatus, "PENDING")
                .set(AgvOrchestrateQueueDO::getErrorMessage, errorMessage));
    }

    /** 重试成功，编排已真正开始，标记 DISPATCHED（不删除，留作追溯记录）。 */
    default void markDispatched(Long id) {
        update(null, new LambdaUpdateWrapper<AgvOrchestrateQueueDO>()
                .eq(AgvOrchestrateQueueDO::getId, id)
                .eq(AgvOrchestrateQueueDO::getStatus, "PENDING")
                .set(AgvOrchestrateQueueDO::getStatus, "DISPATCHED")
                .set(AgvOrchestrateQueueDO::getDispatchTime, LocalDateTime.now()));
    }

    /** 对应流程执行已经不在 RUNNING（被取消/超时/整体失败），放弃继续重试。 */
    default void markCancelled(Long id) {
        update(null, new LambdaUpdateWrapper<AgvOrchestrateQueueDO>()
                .eq(AgvOrchestrateQueueDO::getId, id)
                .eq(AgvOrchestrateQueueDO::getStatus, "PENDING")
                .set(AgvOrchestrateQueueDO::getStatus, "CANCELLED"));
    }
}
