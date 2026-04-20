
package com.librax.lab.module.flow.engine.execution.watchdog;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.exception.ExceptionEngine;
import com.librax.lab.module.flow.engine.execution.exception.FailureActionHelper;
import com.librax.lab.module.flow.engine.execution.exception.FailureDecision;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 超时看门狗
 *
 * <p>定时扫描 RUNNING/WAITING 状态的步骤，发现超时后标记 FAILED，
 * 触发正常的重试/DEAD 链路。
 *
 * <p>超时判断：started_at + timeout_ms < now
 *
 * <p>timeout_ms 来源（按优先级）：
 * <ol>
 *   <li>pd_pipeline_step.timeout_ms（步骤级配置）
 *   <li>pd_pipeline_definition.default_timeout_ms（流程级默认值）
 *   <li>全局兜底值 60000ms（1分钟）
 * </ol>
 *
 * <p>超时后的行为等同于执行失败：走 onStepComplete(FAILED) → 重试/DEAD。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeoutWatchdog {

    private final PipelineExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final PipelineGraphCache graphCache;
    private final ExceptionEngine exceptionEngine;
    private final FailureActionHelper failureActionHelper;

    /**
     * 全局兜底超时（步骤和流程都没配 timeout 时使用）
     */
    private static final long DEFAULT_TIMEOUT_MS = 60_000L;

    /**
     * WAITING 状态的默认超时（设备回调如果长时间不来）
     */
    private static final long DEFAULT_WAITING_TIMEOUT_MS = 300_000L; // 5分钟

    // ================================================================
    // 步骤级超时扫描 — 每 10 秒
    // ================================================================

    @Scheduled(fixedDelay = 10_0000, initialDelay = 30_0000)
    public void scanStepTimeout() {
        // 查所有 RUNNING 状态的流程
        List<PipelineExecutionDO> runningExecutions = executionMapper
                .selectByStatus(ExecutionStatusEnum.RUNNING.name());

        for (PipelineExecutionDO execution : runningExecutions) {
            try {
                checkStepTimeouts(execution);
            } catch (Exception e) {
                log.error("[TimeoutWatchdog] 步骤超时扫描异常 executionId={}",
                        execution.getExecutionId(), e);
            }
        }
    }

    // ================================================================
    // 流程级超时扫描 — 每 30 秒
    // ================================================================

    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    public void scanExecutionTimeout() {
        List<PipelineExecutionDO> runningExecutions = executionMapper
                .selectByStatus(ExecutionStatusEnum.RUNNING.name());

        for (PipelineExecutionDO execution : runningExecutions) {
            try {
                checkExecutionTimeout(execution);
            } catch (Exception e) {
                log.error("[TimeoutWatchdog] 流程超时扫描异常 executionId={}",
                        execution.getExecutionId(), e);
            }
        }
    }

    // ================================================================
    // 步骤超时检查
    // ================================================================

    private void checkStepTimeouts(PipelineExecutionDO execution) {
        String executionId = execution.getExecutionId();

        // 加载流程图（从缓存取 timeout_ms 配置）
        PipelineGraph graph;
        try {
            graph = graphCache.get(execution.getPipelineKey(), execution.getPipelineVersion());
        } catch (Exception e) {
            log.warn("[TimeoutWatchdog] 加载流程图失败 executionId={}", executionId);
            return;
        }

        // 查 RUNNING 和 WAITING 状态的步骤
        List<StepExecutionDO> activeSteps = stepMapper.selectLatestByExecutionId(executionId)
                .stream()
                .filter(s -> StepStatusEnum.RUNNING.name().equals(s.getStatus())
                        || StepStatusEnum.WAITING.name().equals(s.getStatus()))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();

        for (StepExecutionDO step : activeSteps) {
            if (step.getStartedAt() == null) continue;

            // 获取超时配置
            long timeoutMs = resolveTimeoutMs(graph, step, execution);

            // 判断是否超时
            long elapsedMs = Duration.between(step.getStartedAt(), now).toMillis();
            if (elapsedMs > timeoutMs) {
                handleStepTimeout(execution, step, elapsedMs, timeoutMs);
            }
        }
    }

    /**
     * 处理超时步骤（改造后）
     *
     * 原来：CAS标FAILED → exceptionEngine.handleStepFailure(...)
     * 现在：CAS标FAILED → exceptionEngine.decide() → failureActionHelper.execute()
     */
    private void handleStepTimeout(PipelineExecutionDO execution,
                                   StepExecutionDO step,
                                   long elapsedMs,
                                   long timeoutMs) {
        String executionId = execution.getExecutionId();
        String nodeId = step.getNodeId();
        int attempt = step.getAttempt();
        String currentStatus = step.getStatus();

        log.warn("[TimeoutWatchdog] 步骤超时 executionId={} nodeId={} attempt={} " +
                        "status={} elapsed={}ms timeout={}ms",
                executionId, nodeId, attempt, currentStatus, elapsedMs, timeoutMs);

        // CAS: RUNNING/WAITING → FAILED（跟原来完全一样）
        LocalDateTime now = LocalDateTime.now();
        int rows = stepMapper.update(null, new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .in(StepExecutionDO::getStatus, StepStatusEnum.RUNNING.name(),
                        StepStatusEnum.WAITING.name())
                .set(StepExecutionDO::getStatus, StepStatusEnum.FAILED.name())
                .set(StepExecutionDO::getErrorCode, "STEP_TIMEOUT")
                .set(StepExecutionDO::getErrorMsg,
                        String.format("步骤超时: 已执行%dms, 超时阈值%dms", elapsedMs, timeoutMs))
                .set(StepExecutionDO::getFinishedAt, now)
                .set(StepExecutionDO::getExecuteMs, elapsedMs)
                .set(StepExecutionDO::getUpdater, "WATCHDOG")
                .set(StepExecutionDO::getUpdateTime, now));

        if (rows == 0) {
            log.info("[TimeoutWatchdog] 步骤状态已变更，跳过 executionId={} nodeId={}",
                    executionId, nodeId);
            return;
        }

        // ★ 改动：分两步——先决策，再执行
        String errorCode = "STEP_TIMEOUT";
        String errorMsg = String.format("步骤超时: 已执行%dms, 超时阈值%dms", elapsedMs, timeoutMs);

        // 1. 问 ExceptionEngine：下一步怎么办？
        FailureDecision decision = exceptionEngine.decide(
                execution.getPipelineKey(),
                execution.getPipelineVersion(),
                nodeId,
                attempt,
                errorCode);

        // 2. 交给 FailureActionHelper 执行
        PipelineGraph graph = graphCache.get(
                execution.getPipelineKey(),
                execution.getPipelineVersion());

        failureActionHelper.execute(
                executionId, graph, nodeId, attempt,
                errorCode, errorMsg, decision);
    }

    // ================================================================
    // 流程级超时检查
    // ================================================================

    private void checkExecutionTimeout(PipelineExecutionDO execution) {
        if (execution.getStartedAt() == null) return;

        // 获取流程级超时配置
        PipelineGraph graph;
        try {
            graph = graphCache.get(execution.getPipelineKey(), execution.getPipelineVersion());
        } catch (Exception e) {
            return;
        }

        Long executionTimeoutMs = graph.getDefaultTimeoutMs();
        if (executionTimeoutMs == null) return; // 没配流程级超时，不检查

        long elapsedMs = Duration.between(execution.getStartedAt(), LocalDateTime.now()).toMillis();

        // 流程级超时用一个放大系数（步骤总数 × 单步超时），避免误判
        // 或者直接用流程定义上配的值
        if (elapsedMs <= executionTimeoutMs) return;

        log.warn("[TimeoutWatchdog] 流程超时 executionId={} elapsed={}ms timeout={}ms",
                execution.getExecutionId(), elapsedMs, executionTimeoutMs);

        // 把所有 RUNNING/WAITING/PENDING 的步骤标记为 FAILED
        List<StepExecutionDO> activeSteps = stepMapper.selectLatestByExecutionId(
                execution.getExecutionId());

        for (StepExecutionDO step : activeSteps) {
            String status = step.getStatus();
            if (StepStatusEnum.RUNNING.name().equals(status)
                    || StepStatusEnum.WAITING.name().equals(status)
                    || StepStatusEnum.PENDING.name().equals(status)) {

                stepMapper.update(null, new LambdaUpdateWrapper<StepExecutionDO>()
                        .eq(StepExecutionDO::getExecutionId, step.getExecutionId())
                        .eq(StepExecutionDO::getNodeId, step.getNodeId())
                        .eq(StepExecutionDO::getAttempt, step.getAttempt())
                        .eq(StepExecutionDO::getStatus, status)
                        .set(StepExecutionDO::getStatus, StepStatusEnum.FAILED.name())
                        .set(StepExecutionDO::getErrorCode, "EXECUTION_TIMEOUT")
                        .set(StepExecutionDO::getErrorMsg, "流程整体超时")
                        .set(StepExecutionDO::getFinishedAt, LocalDateTime.now())
                        .set(StepExecutionDO::getUpdater, "WATCHDOG")
                        .set(StepExecutionDO::getUpdateTime, LocalDateTime.now()));
            }
        }

        // 流程标记 FAILED
        executionMapper.compareAndSetStatus(
                execution.getExecutionId(),
                ExecutionStatusEnum.RUNNING.name(),
                ExecutionStatusEnum.FAILED.name());

        log.warn("[TimeoutWatchdog] 流程超时已处理 executionId={} → FAILED",
                execution.getExecutionId());
    }

    // ================================================================
    // 超时配置解析
    // ================================================================

    /**
     * 获取步骤的超时时间（ms）
     * <p>
     * 优先级：步骤级 > 流程级默认值 > 全局兜底
     * WAITING 状态使用更长的默认超时（设备回调可能需要更多时间）
     */
    private long resolveTimeoutMs(PipelineGraph graph,
                                  StepExecutionDO step,
                                  PipelineExecutionDO execution) {
        // 1. 从流程图中取步骤级 timeout_ms
        StepNode node = graph.getStep(step.getNodeId());
        if (node != null && node.getTimeoutMs() != null && node.getTimeoutMs() > 0) {
            return node.getTimeoutMs();
        }

        // 2. 流程级默认值
        Long defaultTimeout = graph.getDefaultTimeoutMs();
        if (defaultTimeout != null && defaultTimeout > 0) {
            return defaultTimeout;
        }

        // 3. 全局兜底，WAITING 用更长的超时
        if (StepStatusEnum.WAITING.name().equals(step.getStatus())) {
            return DEFAULT_WAITING_TIMEOUT_MS;
        }
        return DEFAULT_TIMEOUT_MS;
    }
}