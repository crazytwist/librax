package com.librax.lab.module.flow.engine.execution.exception;

import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.enums.FailStrategyEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 异常处理引擎（纯决策，无副作用）
 *
 * <p>根据 errorCode 查询重试策略，结合节点配置，返回 {@link FailureDecision}。
 * 不再持有 DagScheduler / StepStateMachine / ExecutionStateMachine 等执行类引用。
 *
 * <p>调用方拿到决策后自己执行：
 * <ul>
 *   <li>DagScheduler.handleFailure() — 内联执行
 *   <li>FailureActionHelper.execute() — 供 TimeoutWatchdog 等外部调用方使用
 * </ul>
 *
 * <p>改动点（对比改造前）：
 * <ul>
 *   <li>删除依赖：DagScheduler、StepStateMachine、ExecutionStateMachine
 *   <li>删除方法：doRetry()、handleDead()、doAlert()
 *   <li>handleStepFailure() → decide()，只返回值对象
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionEngine {

    private final RetryPolicyRegistry policyRegistry;
    private final PipelineGraphCache graphCache;

    /**
     * 纯决策：根据错误码 + 当前重试次数，返回下一步该怎么做
     *
     * @param pipelineKey     流程标识
     * @param pipelineVersion 流程版本
     * @param nodeId          失败的节点ID
     * @param attempt         当前尝试次数
     * @param errorCode       错误码
     * @return 失败决策结果
     */
    public FailureDecision decide(String pipelineKey,
                                  int pipelineVersion,
                                  String nodeId,
                                  int attempt,
                                  String errorCode) {

        // 1. 查策略
        RetryPolicy policy = policyRegistry.resolve(errorCode);

        // 2. 加载节点配置
        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);
        StepNode node = graph.getStep(nodeId);

        log.info("[ExceptionEngine] 决策 nodeId={} attempt={} errorCode={} " +
                        "retryable={} policy={}",
                nodeId, attempt, errorCode,
                policy.isRetryable(), policy.getDescription());

        // 3. 可重试 且 未耗尽
        if (policy.isRetryable()) {
            int maxAttempts = resolveMaxAttempts(policy, node);
            if (attempt < maxAttempts) {
                long backoffMs = resolveBackoffMs(policy, node);
                return FailureDecision.builder()
                        .action(FailureDecision.Action.RETRY)
                        .nextAttempt(attempt + 1)
                        .backoffMs(backoffMs)
                        .stepKey(node.getStepKey())
                        .stepType(node.getStepType().name())
                        .alertOnFail(policy.isAlertOnFail())
                        .alertLevel(policy.getAlertLevel())
                        .description(policy.getDescription())
                        .build();
            }
            log.warn("[ExceptionEngine] 重试耗尽 nodeId={} attempt={}/{}",
                    nodeId, attempt, maxAttempts);
        }

        // 4. 不可重试 或 重试耗尽 → 看节点 onFailure 策略
        FailStrategyEnum strategy = node.getOnFailure();
        FailureDecision.Action deadAction =
                (strategy == FailStrategyEnum.CONTINUE_ON_FAIL)
                        ? FailureDecision.Action.DEAD_CONTINUE
                        : FailureDecision.Action.DEAD_FAIL_FAST;

        return FailureDecision.builder()
                .action(deadAction)
                .alertOnFail(policy.isAlertOnFail())
                .alertLevel(policy.getAlertLevel())
                .description(policy.getDescription())
                .build();
    }

    // ================================================================
    // 私有：参数解析（纯计算，无副作用）
    // ================================================================

    private int resolveMaxAttempts(RetryPolicy policy, StepNode node) {
        if (policy.getMaxRetries() != null) {
            return policy.getMaxRetries();
        }
        return node.getMaxAttempts();
    }

    private long resolveBackoffMs(RetryPolicy policy, StepNode node) {
        if (policy.getBackoffMs() != null) {
            return policy.getBackoffMs();
        }
        return node.getBackoffMs() != null ? node.getBackoffMs() : 2000L;
    }
}