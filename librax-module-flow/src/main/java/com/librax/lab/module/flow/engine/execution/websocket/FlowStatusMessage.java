package com.librax.lab.module.flow.engine.execution.websocket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程状态 WebSocket 推送消息体
 * <p>
 * 前端按 executionId 过滤，只渲染当前查看的实验。
 */
@Data
@Builder
public class FlowStatusMessage {
    /** 执行实例 ID，前端按此字段过滤 */
    private String executionId;
    /** 事件类型，对应 EventTypeEnum 值：PIPELINE_STARTED / STEP_STARTED / STEP_SUCCESS / … */
    private String eventType;
    /** 步骤节点 ID，流程级事件时为 null */
    private String nodeId;
    /** 重试次数，流程级事件时为 null */
    private Integer attempt;
    /** 步骤类型，流程级事件时为 null */
    private String stepType;
    /** 事件发生时间 */
    private LocalDateTime occurredAt;

    // ── 仅流程启动 ──
    private String pipelineKey;
    private Integer pipelineVersion;

    // ── 仅流程完成 ──
    private Boolean success;

    // ── 仅步骤失败 ──
    private String errorCode;
    private String errorMsg;
}
