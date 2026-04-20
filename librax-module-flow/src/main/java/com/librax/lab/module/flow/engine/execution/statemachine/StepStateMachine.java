package com.librax.lab.module.flow.engine.execution.statemachine;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.module.flow.api.statemachine.StepStateApi;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.executioneventlog.ExecutionEventLogMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.execution.event.ExecutionEventPublisher;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.enums.EventTypeEnum;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import com.librax.lab.module.flow.api.enums.WaitingForEnum;
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
 *   PENDING  → RUNNING（tryStart 抢占）
 *   PENDING  → SKIPPED（CONDITION 未选中分支，直接跳过）
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

    private final StepExecutionMapper stepMapper;
    private final ExecutionEventPublisher eventPublisher;
    private final ExecutionEventLogMapper eventLogMapper;

    /**
     * PENDING → RUNNING：步骤开始执行（乐观锁抢占）
     *
     * <p>并发场景下（如断点恢复 + 正常调度同时触发），多个线程可能同时尝试启动同一步骤。
     * 通过 {@code WHERE status = 'PENDING'} 保证只有一个线程成功，
     * 其他线程拿到 false 后直接丢弃，不报错（正常竞争行为）。
     *
     * @param executionId 流程执行实例ID
     * @param nodeId      节点ID
     * @param attempt     第几次尝试
     * @return true=抢占成功，可以继续执行；false=已被其他线程抢占，直接丢弃
     */
    public boolean tryStart(String executionId, String nodeId, int attempt) {
        LocalDateTime now = LocalDateTime.now();
        String callbackToken = UUID.randomUUID().toString().replace("-", "");

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .eq(StepExecutionDO::getStatus, StepStatusEnum.PENDING.name())
                .set(StepExecutionDO::getStatus, RUNNING.name())
                .set(StepExecutionDO::getStartedAt, now)
                .set(StepExecutionDO::getCallbackToken, callbackToken)
                .set(StepExecutionDO::getUpdater, "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

        boolean acquired = stepMapper.update(null, wrapper) > 0;
        if (acquired) {
            // 查 stepType
            StepExecutionDO current = stepMapper.selectByExecutionNodeAttempt(
                    executionId, nodeId, attempt);
            String stepType = current != null ? current.getStepType() : null;

            log.info("[StepStateMachine] PENDING->RUNNING executionId={} nodeId={} attempt={}",
                    executionId, nodeId, attempt);
            eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                    EventTypeEnum.STEP_STARTED,
                    StepStatusEnum.PENDING.name(), RUNNING.name(),
                    stepType != null ? Map.of("stepType", stepType) : null);
        }
        return acquired;
    }

    /**
     * RUNNING → SUCCESS：步骤执行成功
     *
     * <p>同时记录输出数据（写入 output_data 字段，后续由 ExecutionContextManager 同步到上下文）
     * 和实际执行耗时（{@code execute_ms = finished_at - started_at}）。
     *
     * @param executionId 流程执行实例ID
     * @param nodeId      节点ID
     * @param attempt     第几次尝试
     * @param result      执行结果（含输出数据）
     */
    public void markSuccess(String executionId, String nodeId,
                            int attempt, StepResult result) {

        LocalDateTime now = LocalDateTime.now();
        // ★ 这里已经查了 current，复用它取 stepType
        StepExecutionDO current = stepMapper.selectByExecutionNodeAttempt(
                executionId, nodeId, attempt);
        long executeMs = (current != null && current.getStartedAt() != null)
                ? Duration.between(current.getStartedAt(), now).toMillis() : 0L;
        String stepType = current != null ? current.getStepType() : null;

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .in(StepExecutionDO::getStatus, RUNNING.name(), WAITING.name())
                .set(StepExecutionDO::getStatus, StepStatusEnum.SUCCESS.name())
                .set(StepExecutionDO::getOutputData,
                        result.getOutputs() != null
                                ? JSON.toJSONString(result.getOutputs()) : null)
                .set(StepExecutionDO::getFinishedAt, now)
                .set(StepExecutionDO::getExecuteMs, executeMs)
                .set(StepExecutionDO::getUpdater, "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

        stepMapper.update(null, wrapper);
        log.info("[StepStateMachine] RUNNING->SUCCESS executionId={} nodeId={} attempt={} costMs={}",
                executionId, nodeId, attempt, executeMs);

        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_SUCCESS,
                RUNNING.name(), StepStatusEnum.SUCCESS.name(),
                stepType != null ? Map.of("stepType", stepType) : null);
    }

    /**
     * RUNNING → FAILED：本次尝试失败
     *
     * <p>只标记当前行为 FAILED，不决定是否重试。
     * 重试逻辑由调度器判断（{@code attempt < maxAttempts}），
     * 需要重试则调 {@link #insertRetryRow}，耗尽则调 {@link #markDead}。
     *
     * @param executionId 流程执行实例ID
     * @param nodeId      节点ID
     * @param attempt     第几次尝试
     * @param result      执行结果（含错误码和错误信息）
     */
    public void markFailed(String executionId, String nodeId,
                           int attempt, StepResult result) {
        LocalDateTime now = LocalDateTime.now();
        long executeMs = calcExecuteMs(executionId, nodeId, attempt, now);

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .in(StepExecutionDO::getStatus, RUNNING.name(), WAITING.name())
                .set(StepExecutionDO::getStatus, StepStatusEnum.FAILED.name())
                .set(StepExecutionDO::getErrorCode, result.getErrorCode())
                .set(StepExecutionDO::getErrorMsg, result.getErrorMsg())
                .set(StepExecutionDO::getFinishedAt, now)
                .set(StepExecutionDO::getExecuteMs, executeMs)
                .set(StepExecutionDO::getUpdater, "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

        stepMapper.update(null, wrapper);
        log.warn("[StepStateMachine] RUNNING->FAILED executionId={} nodeId={} attempt={} error={}",
                executionId, nodeId, attempt, result.getErrorMsg());

        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_FAILED,
                RUNNING.name(), StepStatusEnum.FAILED.name(),
                Map.of("errorCode", String.valueOf(result.getErrorCode()),
                        "errorMsg", String.valueOf(result.getErrorMsg())));
    }

    /**
     * 插入重试行（attempt+1，status=PENDING）
     *
     * <p>每次重试 INSERT 新行，旧行保持 FAILED 状态不变，
     * 历史失败记录完整保留，可追溯每次失败的具体原因和耗时。
     *
     * <p>新行的 {@code queued_at} 为当前时间，
     * 后续 {@code wait_ms = started_at - queued_at} 反映的是重试排队等待时间。
     *
     * @param executionId 流程执行实例ID
     * @param nodeId      节点ID
     * @param nextAttempt 下一次尝试次数（当前 attempt + 1）
     * @param stepKey     步骤标识（冗余存，便于按类型统计）
     * @param stepType    步骤类型
     */
    public void insertRetryRow(String executionId, String nodeId,
                               int nextAttempt, String stepKey, String stepType) {
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

    /**
     * FAILED → DEAD：耗尽所有重试次数，步骤彻底失败
     *
     * <p>DEAD 之后由调度器决定后续行为：
     * <ul>
     *   <li>FAIL_FAST     → 终止整条流程（流程状态流转为 FAILED）
     *   <li>CONTINUE      → 跳过此节点，继续调度其他节点
     *   <li>COMPENSATE    → 触发补偿节点
     * </ul>
     *
     * @param executionId 流程执行实例ID
     * @param nodeId      节点ID
     * @param attempt     最后一次尝试次数
     */
    public void markDead(String executionId, String nodeId, int attempt) {
        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .eq(StepExecutionDO::getStatus, StepStatusEnum.FAILED.name())
                .set(StepExecutionDO::getStatus, StepStatusEnum.DEAD.name())
                .set(StepExecutionDO::getUpdater, "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, LocalDateTime.now());

        stepMapper.update(null, wrapper);
        log.warn("[StepStateMachine] FAILED->DEAD executionId={} nodeId={} attempt={}",
                executionId, nodeId, attempt);

        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_DEAD,
                StepStatusEnum.FAILED.name(), StepStatusEnum.DEAD.name(), null);
    }

    /**
     * PENDING → SKIPPED：CONDITION 节点未选中的分支直接跳过
     *
     * <p>跳过的节点不经过 RUNNING 状态，由调度器在分析到条件分支时直接写入终态。
     * SKIPPED 在调度器的依赖判断中等同于 SUCCESS（{@link StepStatusEnum#isDependencySatisfied()}），
     * 保证后续汇聚节点能正常触发。
     *
     * @param executionId 流程执行实例ID
     * @param nodeId      被跳过的节点ID
     * @param attempt     第几次尝试（通常为1）
     */
    public void markSkipped(String executionId, String nodeId, int attempt) {
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .eq(StepExecutionDO::getStatus, StepStatusEnum.PENDING.name())
                .set(StepExecutionDO::getStatus, StepStatusEnum.SKIPPED.name())
                .set(StepExecutionDO::getFinishedAt, now)
                .set(StepExecutionDO::getUpdater, "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

        stepMapper.update(null, wrapper);
        log.info("[StepStateMachine] PENDING->SKIPPED executionId={} nodeId={}",
                executionId, nodeId);

        eventPublisher.publishStepEvent(executionId, nodeId, attempt,
                EventTypeEnum.STEP_SKIPPED,
                StepStatusEnum.PENDING.name(), StepStatusEnum.SKIPPED.name(), null);
    }

    /**
     * 计算步骤实际执行耗时
     * <p>{@code execute_ms = finished_at - started_at}
     */
    private long calcExecuteMs(String executionId, String nodeId,
                               int attempt, LocalDateTime now) {
        StepExecutionDO current = stepMapper
                .selectByExecutionNodeAttempt(executionId, nodeId, attempt);
        if (current == null || current.getStartedAt() == null) return 0L;
        return Duration.between(current.getStartedAt(), now).toMillis();
    }


    /**
     * RUNNING → WAITING（执行器返回等待外部信号）
     */
    public void markWaiting(String executionId,
                            String nodeId,
                            int attempt,
                            WaitingForEnum waitingFor,
                            String callbackToken) {
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<StepExecutionDO> wrapper = new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, executionId)
                .eq(StepExecutionDO::getNodeId, nodeId)
                .eq(StepExecutionDO::getAttempt, attempt)
                .eq(StepExecutionDO::getStatus, RUNNING.name())
                .set(StepExecutionDO::getStatus, WAITING.name())
                .set(StepExecutionDO::getWaitingFor, waitingFor.name())
                .set(StepExecutionDO::getCallbackToken, callbackToken)
                .set(StepExecutionDO::getUpdater, "SYSTEM")
                .set(StepExecutionDO::getUpdateTime, now);

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

    /**
     * 记录日志等待
     */
    private void logEvent(String executionId, String nodeId, int attempt,
                          String eventType, String fromStatus, String toStatus,
                          Map<String, Object> payload) {
        ExecutionEventLogDO log = new ExecutionEventLogDO();
        log.setExecutionId(executionId);
        log.setNodeId(nodeId);
        log.setAttempt(attempt);
        log.setEventType(eventType);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setPayload(payload != null ? JSON.toJSONString(payload) : null);
        log.setOperator("SYSTEM");
        log.setOccurredAt(LocalDateTime.now());
        eventLogMapper.insert(log);
    }
}
