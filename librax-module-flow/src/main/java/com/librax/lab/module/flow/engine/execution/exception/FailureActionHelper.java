package com.librax.lab.module.flow.engine.execution.exception;

import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.execution.scheduler.DagScheduler;
import com.librax.lab.module.flow.engine.execution.statemachine.ExecutionStateMachine;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.infra.mdc.ExecutionMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 失败动作执行器
 *
 * <p>拿到 {@link FailureDecision} 后执行具体动作：重试 / 标记DEAD+终止 / 标记DEAD+继续。
 *
 * <p>设计目的：
 * <ul>
 *   <li>DagScheduler 自己内联这段逻辑（它本来就有所有依赖）
 *   <li>TimeoutWatchdog 等外部调用方通过此 Helper 执行，避免重复代码
 * </ul>
 *
 * <p>依赖方向：
 * <pre>
 *   TimeoutWatchdog → FailureActionHelper → DagScheduler    （单向链）
 *   TimeoutWatchdog → ExceptionEngine                       （单向）
 *   DagScheduler 不注入 FailureActionHelper（自己内联，无反向依赖）
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailureActionHelper {

    private final StepStateMachine stepStateMachine;
    private final ExecutionStateMachine executionStateMachine;
    private final DagScheduler dagScheduler;

    /**
     * 根据决策结果执行具体动作
     *
     * @param executionId 执行实例ID
     * @param graph       流程图（重试调度和CONTINUE需要）
     * @param nodeId      失败的节点ID
     * @param attempt     当前尝试次数
     * @param errorCode   错误码（日志用）
     * @param errorMsg    错误信息（日志用）
     * @param decision    ExceptionEngine 返回的决策结果
     */
    public void execute(String executionId,
                        PipelineGraph graph,
                        String nodeId,
                        int attempt,
                        String errorCode,
                        String errorMsg,
                        FailureDecision decision) {
        ExecutionMdc.set(executionId, nodeId, attempt);

        // 1. 告警
        if (decision.isAlertOnFail()) {
            doAlert(executionId, nodeId, attempt, errorCode, errorMsg, decision);
        }

        // 2. 根据决策执行
        switch (decision.getAction()) {
            case RETRY:
                log.info("[FailureActionHelper] 安排重试 executionId={} nodeId={} " +
                                "nextAttempt={} backoffMs={}",
                        executionId, nodeId,
                        decision.getNextAttempt(), decision.getBackoffMs());

                stepStateMachine.insertRetryRow(
                        executionId, nodeId,
                        decision.getNextAttempt(),
                        decision.getStepKey(),
                        decision.getStepType());

                dagScheduler.scheduleDelayed(executionId, graph, decision.getBackoffMs());
                break;

            case DEAD_FAIL_FAST:
                log.warn("[FailureActionHelper] DEAD+FAIL_FAST executionId={} nodeId={}",
                        executionId, nodeId);

                stepStateMachine.markDead(executionId, nodeId, attempt);
                executionStateMachine.transition(
                        executionId,
                        ExecutionStatusEnum.RUNNING,
                        ExecutionStatusEnum.FAILED);
                break;

            case DEAD_CONTINUE:
                log.warn("[FailureActionHelper] DEAD+CONTINUE executionId={} nodeId={}",
                        executionId, nodeId);

                stepStateMachine.markDead(executionId, nodeId, attempt);
                dagScheduler.scheduleImmediate(executionId, graph);
                break;

            default:
                log.warn("[FailureActionHelper] 未知决策 action={}, 默认 FAIL_FAST",
                        decision.getAction());
                stepStateMachine.markDead(executionId, nodeId, attempt);
                executionStateMachine.transition(
                        executionId,
                        ExecutionStatusEnum.RUNNING,
                        ExecutionStatusEnum.FAILED);
        }
    }

    /**
     * 告警（当前打日志，后续可对接钉钉/企微/邮件）
     */
    private void doAlert(String executionId, String nodeId, int attempt,
                         String errorCode, String errorMsg,
                         FailureDecision decision) {
        log.warn("[ALERT][{}] 步骤异常告警 executionId={} nodeId={} attempt={} " +
                        "errorCode={} errorMsg={} reason={}",
                decision.getAlertLevel(), executionId, nodeId, attempt,
                errorCode, errorMsg, decision.getDescription());
        // TODO: 对接告警通道
    }
}