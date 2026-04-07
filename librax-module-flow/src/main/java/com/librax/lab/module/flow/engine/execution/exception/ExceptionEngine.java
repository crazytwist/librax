
package com.librax.lab.module.flow.engine.execution.exception;

import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.scheduler.DagScheduler;
import com.librax.lab.module.flow.engine.execution.statemachine.ExecutionStateMachine;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.flow.enums.FailStrategyEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 异常处理引擎
 *
 * <p>统一处理所有步骤失败的情况：超时、执行失败、回调失败等。
 * 根据 errorCode 查策略，决定重试还是直接 DEAD。
 *
 * <p>DagScheduler 失败后只调这一个入口，不再自己判断重试逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionEngine {

    private final RetryPolicyRegistry policyRegistry;
    private final StepStateMachine stepStateMachine;
    private final ExecutionStateMachine executionStateMachine;
    private final PipelineGraphCache graphCache;
    private final DagScheduler dagScheduler;

    /**
     * 处理步骤失败
     *
     * 由 DagScheduler.handleFailure 调用（替代原来的重试/DEAD逻辑）
     *
     * @param executionId  执行实例ID
     * @param pipelineKey  流程标识
     * @param pipelineVersion 流程版本
     * @param nodeId       失败的节点ID
     * @param attempt      当前尝试次数
     * @param errorCode    错误码
     * @param errorMsg     错误信息
     */
    public void handleStepFailure(String executionId,
                                  String pipelineKey,
                                  int pipelineVersion,
                                  String nodeId,
                                  int attempt,
                                  String errorCode,
                                  String errorMsg) {

        // 1. 查策略：这种错误要不要重试
        RetryPolicy policy = policyRegistry.resolve(errorCode);

        log.info("[ExceptionEngine] 处理步骤失败 executionId={} nodeId={} attempt={} " +
                        "errorCode={} retryable={} policy={}",
                executionId, nodeId, attempt, errorCode,
                policy.isRetryable(), policy.getDescription());

        // 2. 加载步骤配置（取 maxAttempts、backoffMs）
        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);
        StepNode node = graph.getStep(nodeId);

        // 3. 决定重试还是 DEAD
        if (policy.isRetryable()) {
            int maxAttempts = resolveMaxAttempts(policy, node);
            if (attempt < maxAttempts) {
                // 还能重试
                doRetry(executionId, graph, node, attempt, policy);
                return;
            }
            // 重试耗尽，走 DEAD
            log.warn("[ExceptionEngine] 重试耗尽 executionId={} nodeId={} attempt={}/{}",
                    executionId, nodeId, attempt, maxAttempts);
        } else {
            // 不可重试，直接 DEAD
            log.warn("[ExceptionEngine] 不可重试的错误 executionId={} nodeId={} errorCode={}",
                    executionId, nodeId, errorCode);
        }

        // 4. 标记 DEAD
        stepStateMachine.markDead(executionId, nodeId, attempt);

        // 5. 告警（如果策略要求）
        if (policy.isAlertOnFail()) {
            doAlert(executionId, nodeId, attempt, errorCode, errorMsg, policy);
        }

        // 6. 根据节点失败策略决定流程影响
        handleDead(executionId, graph, node);
    }

    /**
     * 执行重试
     */
    private void doRetry(String executionId,
                         PipelineGraph graph,
                         StepNode node,
                         int attempt,
                         RetryPolicy policy) {
        int nextAttempt = attempt + 1;
        long backoffMs = resolveBackoffMs(policy, node);

        log.info("[ExceptionEngine] 安排重试 executionId={} nodeId={} " +
                        "nextAttempt={} backoffMs={} reason={}",
                executionId, node.getNodeId(), nextAttempt, backoffMs,
                policy.getDescription());

        // 插入重试行
        stepStateMachine.insertRetryRow(
                executionId, node.getNodeId(),
                nextAttempt, node.getStepKey(), node.getStepType().name());

        // 延迟后重新调度
        dagScheduler.scheduleDelayed(executionId, graph, backoffMs);
    }

    /**
     * 处理 DEAD（节点彻底失败后的流程影响）
     */
    private void handleDead(String executionId,
                            PipelineGraph graph,
                            StepNode node) {
        FailStrategyEnum strategy = node.getOnFailure();

        switch (strategy) {
            case FAIL_FAST:
                log.warn("[ExceptionEngine] DEAD+FAIL_FAST 终止流程 executionId={} nodeId={}",
                        executionId, node.getNodeId());
                executionStateMachine.transition(
                        executionId,
                        ExecutionStatusEnum.RUNNING,
                        ExecutionStatusEnum.FAILED);
                break;

            case CONTINUE_ON_FAIL:
                log.warn("[ExceptionEngine] DEAD+CONTINUE 继续调度 executionId={} nodeId={}",
                        executionId, node.getNodeId());
                dagScheduler.scheduleImmediate(executionId, graph);
                break;

            // 将来可加：
            // case COMPENSATE:
            //     compensationEngine.trigger(executionId, node);
            //     break;

            default:
                log.warn("[ExceptionEngine] 未知失败策略 strategy={}, 默认 FAIL_FAST",
                        strategy);
                executionStateMachine.transition(
                        executionId,
                        ExecutionStatusEnum.RUNNING,
                        ExecutionStatusEnum.FAILED);
        }
    }

    /**
     * 告警（当前简单实现：打日志。后续可对接钉钉/企微/邮件）
     */
    private void doAlert(String executionId, String nodeId, int attempt,
                         String errorCode, String errorMsg, RetryPolicy policy) {
        log.warn("[ALERT][{}] 步骤异常告警 executionId={} nodeId={} attempt={} " +
                        "errorCode={} errorMsg={}",
                policy.getAlertLevel(), executionId, nodeId, attempt,
                errorCode, errorMsg);
        // TODO: 对接告警通道
        //   alertService.send(AlertMessage.builder()
        //       .level(policy.getAlertLevel())
        //       .executionId(executionId)
        //       .nodeId(nodeId)
        //       .errorCode(errorCode)
        //       .errorMsg(errorMsg)
        //       .build());
    }

    /**
     * 解析最大重试次数
     * 策略级 > 步骤级
     */
    private int resolveMaxAttempts(RetryPolicy policy, StepNode node) {
        if (policy.getMaxRetries() != null) {
            return policy.getMaxRetries();
        }
        return node.getMaxAttempts();
    }

    /**
     * 解析退避时间
     * 策略级 > 步骤级
     */
    private long resolveBackoffMs(RetryPolicy policy, StepNode node) {
        if (policy.getBackoffMs() != null) {
            return policy.getBackoffMs();
        }
        return node.getBackoffMs() != null ? node.getBackoffMs() : 2000L;
    }
}
