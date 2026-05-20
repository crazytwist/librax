package com.librax.lab.module.flow.engine.execution.websocket;

import com.librax.lab.framework.common.enums.UserTypeEnum;
import com.librax.lab.module.flow.engine.execution.event.*;
import com.librax.lab.module.infra.api.websocket.WebSocketSenderApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 流程执行状态 WebSocket 推送
 *
 * <p>监听 {@link ExecutionEventPublisher} 发布的 Spring 事件（已异步写 DB 后触发），
 * 将执行状态变更推送给前端。
 *
 * <p>推送目标：所有管理员用户。前端按 executionId 过滤。
 *
 * <p>设计要点：
 * <ul>
 *   <li>完全解耦 — 不修改 {@link ExecutionEventPublisher} 或任何引擎代码
 *   <li>异常安全 — 单个事件推送失败不影响后续事件
 *   <li>轻量 — WebSocket 推送本身是异步的，不阻塞事件发布线程
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowStatusWebSocketNotifier {

    private static final String MSG_TYPE = "FLOW_STATUS";

    private final WebSocketSenderApi webSocketSenderApi;

    // ================================================================
    // 流程级事件
    // ================================================================

    @EventListener
    public void onExecutionStarted(ExecutionStartedEvent event) {
        push(FlowStatusMessage.builder()
                .executionId(event.getExecutionId())
                .eventType("PIPELINE_STARTED")
                .pipelineKey(event.getPipelineKey())
                .pipelineVersion(event.getPipelineVersion())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    @EventListener
    public void onExecutionCompleted(ExecutionCompletedEvent event) {
        push(FlowStatusMessage.builder()
                .executionId(event.getExecutionId())
                .eventType(event.isSuccess() ? "PIPELINE_SUCCESS" : "PIPELINE_FAILED")
                .success(event.isSuccess())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // ================================================================
    // 步骤级事件
    // ================================================================

    @EventListener
    public void onStepStarted(StepStartedEvent event) {
        push(FlowStatusMessage.builder()
                .executionId(event.getExecutionId())
                .eventType("STEP_STARTED")
                .nodeId(event.getNodeId())
                .attempt(event.getAttempt())
                .stepType(event.getStepType())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    @EventListener
    public void onStepSuccess(StepSuccessEvent event) {
        push(FlowStatusMessage.builder()
                .executionId(event.getExecutionId())
                .eventType("STEP_SUCCESS")
                .nodeId(event.getNodeId())
                .attempt(event.getAttempt())
                .stepType(event.getStepType())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    @EventListener
    public void onStepFailed(StepFailedEvent event) {
        push(FlowStatusMessage.builder()
                .executionId(event.getExecutionId())
                .eventType("STEP_FAILED")
                .nodeId(event.getNodeId())
                .attempt(event.getAttempt())
                .stepType(event.getStepType())
                .errorCode(event.getErrorCode())
                .errorMsg(event.getErrorMsg())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    @EventListener
    public void onStepWaiting(StepWaitingEvent event) {
        push(FlowStatusMessage.builder()
                .executionId(event.getExecutionId())
                .eventType("STEP_WAITING")
                .nodeId(event.getNodeId())
                .attempt(event.getAttempt())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // ================================================================
    // 推送
    // ================================================================

    private void push(FlowStatusMessage msg) {
        try {
            webSocketSenderApi.sendObject(
                    UserTypeEnum.ADMIN.getValue(),
                    MSG_TYPE,
                    msg);
        } catch (Exception e) {
            log.error("[FlowStatusWS] 推送失败 executionId={} eventType={}",
                    msg.getExecutionId(), msg.getEventType(), e);
        }
    }
}
