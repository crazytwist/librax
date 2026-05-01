package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.exception.ExceptionEngine;
import com.librax.lab.module.flow.engine.execution.exception.FailureDecision;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.engine.execution.statemachine.ExecutionStateMachine;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 步骤失败处理器 — 负责重试、DEAD 决策、资源释放、告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StepFailureHandler {

    private final StepStateMachine stepStateMachine;
    private final ExecutionStateMachine executionStateMachine;
    private final ExceptionEngine exceptionEngine;
    private final StepSubmitter stepSubmitter;

    /**
     * 处理步骤失败
     */
    public void handle(String executionId,
                       PipelineGraph graph,
                       StepNode node,
                       int attempt,
                       StepResult result,
                       java.util.function.Consumer<Long> scheduleDelayed,
                       Runnable scheduleImmediate) {
        // 1. 标记步骤 FAILED
        stepStateMachine.markFailed(executionId, node.getNodeId(), attempt, result);

        // 2. 问异常引擎"下一步怎么办"（纯决策，无副作用）
        FailureDecision decision = exceptionEngine.decide(
                graph.getPipelineKey(),
                graph.getVersion(),
                node.getNodeId(),
                attempt,
                result.getErrorCode());

        // 3. 告警
        if (decision.isAlertOnFail()) {
            log.warn("[ALERT][{}] executionId={} nodeId={} attempt={} " +
                            "errorCode={} errorMsg={} reason={}",
                    decision.getAlertLevel(), executionId,
                    node.getNodeId(), attempt,
                    result.getErrorCode(), result.getErrorMsg(),
                    decision.getDescription());
        }

        // 4. 根据决策执行
        switch (decision.getAction()) {
            case RETRY:
                // 重试不释放资源：下次重试还需要申请，由下一次 submit 重新走申请流程
                // 但当前这次 attempt 的资源需要释放，否则下次申请时会冲突
                stepSubmitter.releaseIfHeld(executionId, node.getNodeId(), attempt, "STEP_RETRY");
                handleRetry(executionId, node, decision, scheduleDelayed);
                break;

            case DEAD_FAIL_FAST:
                // ★ DEAD 终态释放资源
                stepSubmitter.releaseIfHeld(executionId, node.getNodeId(), attempt, "STEP_DEAD");
                handleDeadFailFast(executionId, node, attempt);
                break;

            case DEAD_CONTINUE:
                // ★ DEAD 终态释放资源
                stepSubmitter.releaseIfHeld(executionId, node.getNodeId(), attempt, "STEP_DEAD");
                handleDeadContinue(executionId, node, attempt, scheduleImmediate);
                break;

            default:
                log.warn("[StepFailureHandler] 未知决策 action={}，默认 FAIL_FAST",
                        decision.getAction());
                stepSubmitter.releaseIfHeld(executionId, node.getNodeId(), attempt, "STEP_DEAD");
                handleDeadFailFast(executionId, node, attempt);
        }
    }

    /**
     * 处理重试：插入重试记录 + 延迟调度
     */
    private void handleRetry(String executionId,
                             StepNode node,
                             FailureDecision decision,
                             java.util.function.Consumer<Long> scheduleDelayed) {
        log.info("[StepFailureHandler] 安排重试 executionId={} nodeId={} " +
                        "nextAttempt={} backoffMs={}",
                executionId, node.getNodeId(),
                decision.getNextAttempt(), decision.getBackoffMs());

        stepStateMachine.insertRetryRow(
                executionId, node.getNodeId(),
                decision.getNextAttempt(),
                decision.getStepKey(),
                decision.getStepType());

        scheduleDelayed.accept(decision.getBackoffMs());
    }

    /**
     * 处理 DEAD + FAIL_FAST：标记 DEAD，流程失败
     */
    private void handleDeadFailFast(String executionId,
                                    StepNode node,
                                    int attempt) {
        log.warn("[StepFailureHandler] DEAD+FAIL_FAST executionId={} nodeId={}",
                executionId, node.getNodeId());

        stepStateMachine.markDead(executionId, node.getNodeId(), attempt);
        executionStateMachine.transition(
                executionId,
                ExecutionStatusEnum.RUNNING,
                ExecutionStatusEnum.FAILED);
    }

    /**
     * 处理 DEAD + CONTINUE：标记 DEAD，继续调度其他节点
     */
    private void handleDeadContinue(String executionId,
                                    StepNode node,
                                    int attempt,
                                    Runnable scheduleImmediate) {
        log.warn("[StepFailureHandler] DEAD+CONTINUE executionId={} nodeId={}",
                executionId, node.getNodeId());

        stepStateMachine.markDead(executionId, node.getNodeId(), attempt);
        scheduleImmediate.run();
    }
}