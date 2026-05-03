package com.librax.lab.module.flow.engine.execution.statemachine;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.module.flow.api.enums.WaitingForEnum;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.api.statemachine.StepStateApi;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.executioneventlog.ExecutionEventLogMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.execution.event.ExecutionEventPublisher;
import com.librax.lab.module.flow.enums.EventTypeEnum;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import com.librax.lab.module.infra.mdc.ExecutionMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.librax.lab.module.flow.enums.EventTypeEnum.STEP_WAITING;
import static com.librax.lab.module.flow.enums.StepStatusEnum.RUNNING;
import static com.librax.lab.module.flow.enums.StepStatusEnum.WAITING;

/**
 * 步骤执行状态机
 *
 * <p>职责：管理 {@code pe_step_execution.status} 的所有合法流转。
 * 所有步骤级状态变更必须经过此类，禁止直接 update status 字段。
 *
 * <p>重试设计：每次重试 INSERT 新行（attempt+1），不更新旧行。
 * 历史失败记录完整保留，可追溯每次失败的具体原因。
 *
 * <p>合法流转关系：
 * <pre>
 *   PENDING  → RUNNING（tryStart 抢占，同时写 input_snapshot）
 *   PENDING  → SKIPPED（CONDITION 未选中分支，直接跳过）
 *   RUNNING  → WAITING（等待外部回调）
 *   RUNNING  → SUCCESS
 *   RUNNING  → FAILED → 有重试次数时 INSERT 新 PENDING 行
 *                      → 耗尽次数时 → DEAD
 *   DEAD     → COMPENSATING → COMPENSATED
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StepStateMachine implements StepStateApi {

    private final StepExecutionMapper     stepMapper;
    private final ExecutionEventPublisher eventPublisher;
    private final ExecutionEventLogMapper eventLogMapper;

    // ================================================================
    // tryStart — PENDING → RUNNING
    // ================================================================

    /**
     * PENDING → RUNNING：步骤开始执行（乐观锁抢占）
     *
     * <p>并发场景下（如断点恢复 + 正常调度同时触发），多个线程可能同时尝试启动同一步骤。
     * 通过 {@code WHERE status = 'PENDING'} 保证只有一个线程成功。
     *
     * <p>同时写入 {@code input_snapshot}（执行前入参快照）和 {@code callback_token}，
     * 保证快照与状态变更原子完成。
     *
     * @param executionId   流程执行实例ID
     * @param nodeId        节点ID
     * @param attempt       第几次尝试
     * @param inputParams   步骤实际执行的入参，用于写 input_snapshot（可为 null）
     * @return true=抢占成功；false=已被其他线程抢占，直接丢弃
     */
    public boolean tryStart(String executionId, String nodeId,
                            int attempt, Map<String, Object> inputParams) {
        ExecutionMdc.set(executionId, nodeId, attempt);
        LocalDateTime now           = LocalDateTime.now();
        String        callbackToken = UUID.randomUUID().toString().replace("-", "");
        String        snapshot      = (inputParams != null && !inputParams.isEmpty())
                ? JSON.toJSONString(inputParams) : null;

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId,      nodeId)
                .eq(StepExecutionDO::getAttempt,     attempt)
                .eq(StepExecutionDO::getStatus,      StepStatusEnum.PENDING.name())
                .set(StepExecutionDO::getStatus,         RUNNING.name())
                .set(StepExecutionDO::getStartedAt,      now)
                .set(StepExecutionDO::getCallbackToken,  callbackToken)
                .set(StepExecutionDO::getInputSnapshot,  snapshot)
                .set(StepExecutionDO::getUpdater,        "SYSTEM")
                .set(StepExecutionDO::getUpdateTime,     now);

        boolean acquired = stepMapper.update(null, wrapper) > 0;
        if (!acquired) {
            return false;
        }

        // 查 stepType 用于事件发布（复用已有查询，不额外增加 DB 调用）
        StepExecutionDO current = stepMapper.selectByExecutionNodeAttempt(
                executionId, nodeId, attempt);
        String stepType = current != null ? current.getStepType() : null;

        log.info("[StepStateMachine] PENDING->RUNNING executionId={} nodeId={} attempt={}",
                executionId, nodeId, attempt);
        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_STARTED,
                StepStatusEnum.PENDING.name(), RUNNING.name(),
                stepType != null ? Map.of("stepType", stepType) : null);
        return true;
    }

    /**
     * 兼容旧调用方（无 inputParams 参数），snapshot 写 null
     */
    public boolean tryStart(String executionId, String nodeId, int attempt) {
        return tryStart(executionId, nodeId, attempt, null);
    }

    // ================================================================
    // markSuccess — RUNNING/WAITING → SUCCESS
    // ================================================================

    /**
     * RUNNING/WAITING → SUCCESS：步骤执行成功
     *
     * <p>同时写入输出数据（{@code output_data}）和实际执行耗时（{@code execute_ms}）。
     */
    public void markSuccess(String executionId, String nodeId,
                            int attempt, StepResult result) {
        ExecutionMdc.set(executionId, nodeId, attempt);
        LocalDateTime    now     = LocalDateTime.now();
        StepExecutionDO  current = stepMapper.selectByExecutionNodeAttempt(
                executionId, nodeId, attempt);
        long   executeMs = calcMs(current, now);
        String stepType  = current != null ? current.getStepType() : null;

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId,      nodeId)
                .eq(StepExecutionDO::getAttempt,     attempt)
                .in(StepExecutionDO::getStatus, RUNNING.name(), WAITING.name())
                .set(StepExecutionDO::getStatus,     StepStatusEnum.SUCCESS.name())
                .set(StepExecutionDO::getOutputData,
                        result.getOutputs() != null
                                ? JSON.toJSONString(result.getOutputs()) : null)
                .set(StepExecutionDO::getFinishedAt, now)
                .set(StepExecutionDO::getExecuteMs,  executeMs)
                .set(StepExecutionDO::getUpdater,    "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

        stepMapper.update(null, wrapper);
        log.info("[StepStateMachine] RUNNING->SUCCESS executionId={} nodeId={} attempt={} costMs={}",
                executionId, nodeId, attempt, executeMs);

        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_SUCCESS,
                RUNNING.name(), StepStatusEnum.SUCCESS.name(),
                stepType != null ? Map.of("stepType", stepType) : null);
    }

    // ================================================================
    // markFailed — RUNNING/WAITING → FAILED
    // ================================================================

    /**
     * RUNNING/WAITING → FAILED：本次尝试失败
     *
     * <p>只标记当前行为 FAILED，不决定是否重试。
     * 重试逻辑由 {@link com.librax.lab.module.flow.engine.execution.scheduler.StepFailureHandler} 决策。
     */
    public void markFailed(String executionId, String nodeId,
                           int attempt, StepResult result) {
        ExecutionMdc.set(executionId, nodeId, attempt);
        LocalDateTime now       = LocalDateTime.now();
        long          executeMs = calcExecuteMs(executionId, nodeId, attempt, now);

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId,      nodeId)
                .eq(StepExecutionDO::getAttempt,     attempt)
                .in(StepExecutionDO::getStatus, RUNNING.name(), WAITING.name())
                .set(StepExecutionDO::getStatus,     StepStatusEnum.FAILED.name())
                .set(StepExecutionDO::getErrorCode,  result.getErrorCode())
                .set(StepExecutionDO::getErrorMsg,   result.getErrorMsg())
                .set(StepExecutionDO::getFinishedAt, now)
                .set(StepExecutionDO::getExecuteMs,  executeMs)
                .set(StepExecutionDO::getUpdater,    "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

        stepMapper.update(null, wrapper);
        log.warn("[StepStateMachine] RUNNING->FAILED executionId={} nodeId={} attempt={} error={}",
                executionId, nodeId, attempt, result.getErrorMsg());

        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_FAILED,
                RUNNING.name(), StepStatusEnum.FAILED.name(),
                Map.of("errorCode", String.valueOf(result.getErrorCode()),
                        "errorMsg",  String.valueOf(result.getErrorMsg())));
    }

    // ================================================================
    // insertRetryRow — 插入重试行
    // ================================================================

    /**
     * 插入重试行（attempt+1，status=PENDING）
     *
     * <p>每次重试 INSERT 新行，旧行保持 FAILED 状态不变，历史记录完整保留。
     */
    public void insertRetryRow(String executionId, String nodeId,
                               int nextAttempt, String stepKey, String stepType) {
        ExecutionMdc.set(executionId, nodeId, nextAttempt);
        LocalDateTime now = LocalDateTime.now();

        StepExecutionDO retryRow = new StepExecutionDO();
        retryRow.setExecutionId(executionId);
        retryRow.setNodeId(nodeId);
        retryRow.setStepKey(stepKey);
        retryRow.setStepType(stepType);
        retryRow.setAttempt(nextAttempt);
        retryRow.setStatus(StepStatusEnum.PENDING.name());
        retryRow.setRunMode("NORMAL");
        retryRow.setQueuedAt(now);

        stepMapper.insert(retryRow);
        log.info("[StepStateMachine] 重试行已插入 executionId={} nodeId={} nextAttempt={}",
                executionId, nodeId, nextAttempt);

        eventPublisher.publishStepEvent(executionId, nodeId, nextAttempt,
                EventTypeEnum.STEP_RETRY_SCHEDULED,
                StepStatusEnum.FAILED.name(), StepStatusEnum.PENDING.name(), null);
    }

    // ================================================================
    // markDead — FAILED → DEAD
    // ================================================================

    /**
     * FAILED → DEAD：耗尽所有重试次数，步骤彻底失败
     */
    public void markDead(String executionId, String nodeId, int attempt) {
        ExecutionMdc.set(executionId, nodeId, attempt);
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId,      nodeId)
                .eq(StepExecutionDO::getAttempt,     attempt)
                .eq(StepExecutionDO::getStatus,      StepStatusEnum.FAILED.name())
                .set(StepExecutionDO::getStatus,     StepStatusEnum.DEAD.name())
                .set(StepExecutionDO::getUpdater,    "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

        stepMapper.update(null, wrapper);
        log.warn("[StepStateMachine] FAILED->DEAD executionId={} nodeId={} attempt={}",
                executionId, nodeId, attempt);

        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_DEAD,
                StepStatusEnum.FAILED.name(), StepStatusEnum.DEAD.name(), null);
    }

    // ================================================================
    // markSkipped — PENDING → SKIPPED
    // ================================================================

    /**
     * PENDING → SKIPPED：CONDITION 节点未选中的分支直接跳过
     */
    public void markSkipped(String executionId, String nodeId, int attempt) {
        ExecutionMdc.set(executionId, nodeId, attempt);
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId,      nodeId)
                .eq(StepExecutionDO::getAttempt,     attempt)
                .eq(StepExecutionDO::getStatus,      StepStatusEnum.PENDING.name())
                .set(StepExecutionDO::getStatus,     StepStatusEnum.SKIPPED.name())
                .set(StepExecutionDO::getFinishedAt, now)
                .set(StepExecutionDO::getUpdater,    "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

        stepMapper.update(null, wrapper);
        log.info("[StepStateMachine] PENDING->SKIPPED executionId={} nodeId={}",
                executionId, nodeId);

        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_SKIPPED,
                StepStatusEnum.PENDING.name(), StepStatusEnum.SKIPPED.name(), null);
    }

    // ================================================================
    // markWaiting — RUNNING → WAITING
    // ================================================================

    /**
     * RUNNING → WAITING：执行器返回等待外部信号（设备回调 / 人工审批）
     */
    public void markWaiting(String executionId, String nodeId,
                            int attempt, WaitingForEnum waitingFor,
                            String callbackToken) {
        ExecutionMdc.set(executionId, nodeId, attempt);
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId,      nodeId)
                .eq(StepExecutionDO::getAttempt,     attempt)
                .eq(StepExecutionDO::getStatus,      RUNNING.name())
                .set(StepExecutionDO::getStatus,        WAITING.name())
                .set(StepExecutionDO::getWaitingFor,    waitingFor.name())
                .set(StepExecutionDO::getCallbackToken, callbackToken)
                .set(StepExecutionDO::getUpdater,       "SYSTEM")
                .set(StepExecutionDO::getUpdateTime,    now);

        int rows = stepMapper.update(null, wrapper);
        if (rows > 0) {
            log.info("[StepStateMachine] RUNNING->WAITING executionId={} nodeId={} " +
                            "attempt={} waitingFor={}",
                    executionId, nodeId, attempt, waitingFor);
            logEvent(executionId, nodeId, attempt,
                    STEP_WAITING.name(), RUNNING.name(), WAITING.name(),
                    Map.of("waitingFor", waitingFor.name()));
        }
    }

    // ================================================================
    // markResourceAcquired / clearResourceAcquired
    // ================================================================

    /**
     * 标记步骤已申请到资源（宕机恢复时判断是否需要先释放）
     */
    public int markResourceAcquired(String executionId, String nodeId, int attempt) {
        return stepMapper.update(null, new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId,      nodeId)
                .eq(StepExecutionDO::getAttempt,     attempt)
                .set(StepExecutionDO::getResourceAcquired, 1)
                .set(StepExecutionDO::getUpdater,    "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 清除资源申请标记（资源释放后清零）
     */
    public int clearResourceAcquired(String executionId, String nodeId, int attempt) {
        return stepMapper.update(null, new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId,      nodeId)
                .eq(StepExecutionDO::getAttempt,     attempt)
                .set(StepExecutionDO::getResourceAcquired, 0)
                .set(StepExecutionDO::getUpdater,    "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, LocalDateTime.now()));
    }

    // ================================================================
    // 私有工具方法
    // ================================================================

    /** 从已有 DO 计算耗时（避免重复查库） */
    private long calcMs(StepExecutionDO current, LocalDateTime now) {
        if (current == null || current.getStartedAt() == null) return 0L;
        return Duration.between(current.getStartedAt(), now).toMillis();
    }

    /** 查库后计算耗时 */
    private long calcExecuteMs(String executionId, String nodeId,
                               int attempt, LocalDateTime now) {
        StepExecutionDO current = stepMapper.selectByExecutionNodeAttempt(
                executionId, nodeId, attempt);
        return calcMs(current, now);
    }

    /** 写事件日志（markWaiting 专用，其他状态走 eventPublisher） */
    private void logEvent(String executionId, String nodeId, int attempt,
                          String eventType, String fromStatus, String toStatus,
                          Map<String, Object> payload) {
        ExecutionEventLogDO record = new ExecutionEventLogDO();
        record.setExecutionId(executionId);
        record.setNodeId(nodeId);
        record.setAttempt(attempt);
        record.setEventType(eventType);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setPayload(payload != null ? JSON.toJSONString(payload) : null);
        record.setOperator("SYSTEM");
        record.setOccurredAt(LocalDateTime.now());
        eventLogMapper.insert(record);
    }
}