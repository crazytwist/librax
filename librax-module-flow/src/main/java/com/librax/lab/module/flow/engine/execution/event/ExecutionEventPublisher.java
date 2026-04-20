package com.librax.lab.module.flow.engine.execution.event;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import com.librax.lab.module.flow.dal.mysql.executioneventlog.ExecutionEventLogMapper;
import com.librax.lab.module.flow.enums.EventTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionEventPublisher {

    private final ExecutionEventLogMapper eventLogMapper;
    private final ApplicationEventPublisher springEventPublisher; // ★ 新增

    /**
     * 发布流程级事件
     */
    @Async("labEventListenerExecutor")
    public void publishPipelineEvent(String executionId,
                                     String nodeId,
                                     EventTypeEnum eventType,
                                     String fromStatus,
                                     String toStatus,
                                     Map<String, Object> payload) {
        // 写 DB 日志（原有逻辑不变）
        doPublish(executionId, nodeId, null, null,
                eventType, fromStatus, toStatus, payload);

        // ★ 新增：发布 Spring Event
        publishSpringPipelineEvent(executionId, eventType, payload);
    }

    /**
     * 发布步骤级事件
     */
    @Async("labEventListenerExecutor")
    public void publishStepEvent(String executionId,
                                 String nodeId,
                                 Integer attempt,
                                 EventTypeEnum eventType,
                                 String fromStatus,
                                 String toStatus,
                                 Map<String, Object> payload) {
        // 写 DB 日志（原有逻辑不变）
        doPublish(executionId, nodeId, attempt, null,
                eventType, fromStatus, toStatus, payload);

        // ★ 新增：发布 Spring Event
        publishSpringStepEvent(executionId, nodeId, attempt, eventType, payload);
    }

    // ================================================================
    // Spring Event 发布（新增）
    // ================================================================

    private void publishSpringPipelineEvent(String executionId,
                                            EventTypeEnum eventType,
                                            Map<String, Object> payload) {
        try {
            switch (eventType) {
                case PIPELINE_STARTED:
                    String pipelineKey = payload != null ?
                            (String) payload.get("pipelineKey") : null;
                    Integer version = payload != null ?
                            (Integer) payload.get("pipelineVersion") : null;
                    springEventPublisher.publishEvent(
                            new ExecutionStartedEvent(this, executionId,
                                    pipelineKey, version != null ? version : 0));
                    break;

                case PIPELINE_SUCCESS:
                    springEventPublisher.publishEvent(
                            new ExecutionCompletedEvent(this, executionId, true));
                    break;

                case PIPELINE_FAILED:
                    springEventPublisher.publishEvent(
                            new ExecutionCompletedEvent(this, executionId, false));
                    break;

                default:
                    // 其他流程级事件暂不发布 Spring Event
                    break;
            }
        } catch (Exception e) {
            log.error("[EventPublisher] Spring Event 发布失败 executionId={} type={}",
                    executionId, eventType, e);
        }
    }

    private void publishSpringStepEvent(String executionId,
                                        String nodeId,
                                        Integer attempt,
                                        EventTypeEnum eventType,
                                        Map<String, Object> payload) {
        try {
            int att = attempt != null ? attempt : 1;
            String stepType = payload != null ?
                    (String) payload.get("stepType") : null;

            switch (eventType) {
                case STEP_STARTED:
                    springEventPublisher.publishEvent(
                            new StepStartedEvent(this, executionId,
                                    nodeId, att, stepType));
                    break;

                case STEP_SUCCESS:
                    springEventPublisher.publishEvent(
                            new StepSuccessEvent(this, executionId,
                                    nodeId, att, stepType));
                    break;

                case STEP_FAILED:
                    String errorCode = payload != null ?
                            (String) payload.get("errorCode") : null;
                    String errorMsg = payload != null ?
                            (String) payload.get("errorMsg") : null;
                    springEventPublisher.publishEvent(
                            new StepFailedEvent(this, executionId,
                                    nodeId, att, stepType,
                                    errorCode, errorMsg));
                    break;

                case STEP_WAITING:
                    String waitingFor = payload != null ?
                            (String) payload.get("waitingFor") : null;
                    springEventPublisher.publishEvent(
                            new StepWaitingEvent(this, executionId,
                                    nodeId, att, waitingFor));
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            log.error("[EventPublisher] Spring Event 发布失败 executionId={} type={}",
                    executionId, eventType, e);
        }
    }

    // ================================================================
    // 原有的 DB 写入逻辑（完全不变）
    // ================================================================

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
            log.error("[ExecutionEventPublisher] DB写入失败 executionId={} eventType={}",
                    executionId, eventType, e.getMessage());
        }
    }
}