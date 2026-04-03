package com.librax.lab.module.flow.engine.execution.event;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import com.librax.lab.module.flow.dal.mysql.executioneventlog.ExecutionEventLogMapper;
import com.librax.lab.module.flow.enums.EventTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionEventPublisher {

    private final ExecutionEventLogMapper eventLogMapper;

    /**
     * 发布流程级事件（PIPELINE_STARTED / PAUSED / SUCCESS 等）
     *
     * @param executionId 执行实例ID
     * @param nodeId      流程级事件传 null
     * @param eventType   事件类型
     * @param fromStatus  变更前状态
     * @param toStatus    变更后状态
     * @param payload     附加数据，可为 null
     */
    @Async
    public void publishPipelineEvent(String executionId,
                                     String nodeId,
                                     EventTypeEnum eventType,
                                     String fromStatus,
                                     String toStatus,
                                     Map<String, Object> payload) {
        doPublish(executionId, nodeId, null, null,
                eventType, fromStatus, toStatus, payload);
    }

    /**
     * 发布步骤级事件（STEP_STARTED / SUCCESS / FAILED 等）
     *
     * @param executionId 执行实例ID
     * @param nodeId      节点ID
     * @param attempt     第几次尝试
     * @param eventType   事件类型
     * @param fromStatus  变更前状态
     * @param toStatus    变更后状态
     * @param payload     附加数据，可为 null
     */
    @Async
    public void publishStepEvent(String executionId,
                                 String nodeId,
                                 Integer attempt,
                                 EventTypeEnum eventType,
                                 String fromStatus,
                                 String toStatus,
                                 Map<String, Object> payload) {
        doPublish(executionId, nodeId, attempt, null,
                eventType, fromStatus, toStatus, payload);
    }

    // ----------------------------------------------------------------
    // 内部实现：构建 DO 并写库，捕获所有异常防止影响主流程
    // ----------------------------------------------------------------
    private void doPublish(String executionId,
                           String nodeId,
                           Integer attempt,
                           String runMode,
                           EventTypeEnum eventType,
                           String fromStatus,
                           String toStatus,
                           Map<String, Object> payload) {
        try {
            LocalDateTime now = LocalDateTime.now();
            ExecutionEventLogDO record = ExecutionEventLogDO.builder()
                    .executionId(executionId)
                    .nodeId(nodeId)
                    .attempt(attempt)
                    .runMode(runMode)
                    .eventType(eventType.name())
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .payload(payload != null ? JSON.toJSONString(payload) : null)
                    .operator("SYSTEM")
                    .occurredAt(now)
                    .build();
            record.setCreator("SYSTEM");
            record.setUpdater("SYSTEM");
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setDeleted(false);


            eventLogMapper.insert(record);

        } catch (Exception e) {
            // 事件日志写入失败不能影响主流程，降级为日志记录
            log.error("[ExecutionEventPublisher] 写入失败 executionId={} eventType={} error={}",
                    executionId, eventType, e.getMessage());
        }
    }
}