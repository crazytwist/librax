
package com.librax.lab.module.flow.engine.execution.statemachine;

import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.engine.execution.event.ExecutionEventPublisher;
import com.librax.lab.module.flow.enums.EventTypeEnum;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.infra.mdc.ExecutionMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.PIPELINE_EXECUTION_STATUS_INVALID;

/**
 * 流程执行实例状态机
 *
 * <p>职责：管理 {@code pe_pipeline_execution.status} 的所有合法流转。
 * 所有流程级状态变更必须经过此类，禁止直接 update status 字段。
 *
 * <p>合法流转关系：
 * <pre>
 *   PENDING      → RUNNING / CANCELLED
 *   RUNNING      → PAUSED / SUCCESS / FAILED / CANCELLED / COMPENSATING
 *   PAUSED       → RUNNING / CANCELLED
 *   FAILED       → COMPENSATING / RUNNING（手动重跑）
 *   COMPENSATING → COMPENSATED / FAILED
 *   SUCCESS / CANCELLED / COMPENSATED → 终态，不可再流转
 * </pre>
 *
 * <p>并发安全：使用乐观锁（{@code WHERE status = fromStatus}）防止并发覆盖，
 * 影响行数为0时说明状态已被其他线程修改，抛出异常由调用方处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionStateMachine {

    private final PipelineExecutionMapper executionMapper;
    private final ExecutionEventPublisher eventPublisher;

    /**
     * 合法状态流转表
     * key   = 当前状态
     * value = 允许流转到的目标状态集合
     */
    private static final Map<ExecutionStatusEnum, Set<ExecutionStatusEnum>> TRANSITIONS = Map.of(
            ExecutionStatusEnum.PENDING, Set.of(
                    ExecutionStatusEnum.RUNNING,
                    ExecutionStatusEnum.CANCELLED),
            ExecutionStatusEnum.RUNNING, Set.of(
                    ExecutionStatusEnum.PAUSED,
                    ExecutionStatusEnum.SUCCESS,
                    ExecutionStatusEnum.FAILED,
                    ExecutionStatusEnum.CANCELLED,
                    ExecutionStatusEnum.COMPENSATING),
            ExecutionStatusEnum.PAUSED, Set.of(
                    ExecutionStatusEnum.RUNNING,
                    ExecutionStatusEnum.CANCELLED),
            ExecutionStatusEnum.FAILED, Set.of(
                    ExecutionStatusEnum.COMPENSATING,
                    ExecutionStatusEnum.RUNNING),
            ExecutionStatusEnum.COMPENSATING, Set.of(
                    ExecutionStatusEnum.COMPENSATED,
                    ExecutionStatusEnum.FAILED),
            ExecutionStatusEnum.SUCCESS, Set.of(),
            ExecutionStatusEnum.CANCELLED, Set.of(),
            ExecutionStatusEnum.COMPENSATED, Set.of()
    );

    /**
     * 执行流程状态流转
     *
     * <p>内部使用乐观锁：{@code WHERE status = fromStatus}，
     * 保证同一时刻只有一个线程能成功流转，避免并发状态覆盖。
     *
     * <p>流转成功后异步写入事件日志 {@code pe_execution_event_log}。
     *
     * @param executionId 流程执行实例ID
     * @param from        期望的当前状态（乐观锁条件）
     * @param to          目标状态
     * @throws com.librax.lab.framework.common.exception.ServiceException 非法流转（from→to 不在合法表中）或并发冲突时抛出
     */
    public void transition(String executionId,
                           ExecutionStatusEnum from,
                           ExecutionStatusEnum to) {
        ExecutionMdc.set(executionId);
        // 1. 校验流转合法性
        if (!canTransition(from, to)) {
            throw exception(PIPELINE_EXECUTION_STATUS_INVALID,
                    from.name() + " -> " + to.name());
        }

        // 2. 乐观锁更新（SQL 内同时处理 started_at / finished_at）
        int updated = executionMapper.compareAndSetStatus(
                executionId, from.name(), to.name());

        // 3. updated=0 说明 status 已不是期望的 from，被其他线程抢先修改
        if (updated == 0) {
            PipelineExecutionDO current = executionMapper.selectByExecutionId(executionId);
            String actual = current != null ? current.getStatus() : "NOT_FOUND";
            log.warn("[ExecutionStateMachine] 并发冲突 executionId={} expectedFrom={} actual={}",
                    executionId, from, actual);
            throw exception(PIPELINE_EXECUTION_STATUS_INVALID,
                    from + " -> " + to + "（实际状态: " + actual + "）");
        }

        log.info("[ExecutionStateMachine] 状态流转成功 executionId={} {} -> {}",
                executionId, from, to);

        // transition 方法最后，加一个判断：不重复发 PIPELINE_STARTED
        // 因为 PENDING→RUNNING 的事件由 PipelineExecutionServiceImpl.afterCommit 发布（带完整 payload）
        if (from == ExecutionStatusEnum.PENDING && to == ExecutionStatusEnum.RUNNING) {
            // 跳过，由 start() 的 afterCommit 发布（带 pipelineKey 和 version）
            return;
        }
        if (from == ExecutionStatusEnum.RUNNING
                && (to == ExecutionStatusEnum.SUCCESS || to == ExecutionStatusEnum.FAILED)) {
            return; // 由 DagScheduler.finishPipeline 发布
        }

        // 4. 异步写事件日志（不阻塞主链路）
        eventPublisher.publishPipelineEvent(
                executionId, null,
                resolveEventType(to),
                from.name(), to.name(), null);
    }

    /**
     * 仅校验流转是否合法，不执行任何数据库操作
     * <p>供上层在执行前提前判断，避免不必要的 DB 操作
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return true=合法，false=非法
     */
    public boolean canTransition(ExecutionStatusEnum from, ExecutionStatusEnum to) {
        Set<ExecutionStatusEnum> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 根据目标状态推断对应的事件类型
     */
    private EventTypeEnum resolveEventType(ExecutionStatusEnum to) {
        return switch (to) {
            case RUNNING -> EventTypeEnum.PIPELINE_STARTED;
            case PAUSED -> EventTypeEnum.PIPELINE_PAUSED;
            case SUCCESS -> EventTypeEnum.PIPELINE_SUCCESS;
            case FAILED -> EventTypeEnum.PIPELINE_FAILED;
            case CANCELLED -> EventTypeEnum.PIPELINE_CANCELLED;
            case COMPENSATING -> EventTypeEnum.STEP_COMPENSATE_TRIGGERED;
            default -> EventTypeEnum.PIPELINE_STARTED;
        };
    }
}