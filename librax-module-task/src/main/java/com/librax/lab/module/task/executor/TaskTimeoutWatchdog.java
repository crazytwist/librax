package com.librax.lab.module.task.executor;

import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.dispatch.TaskCallbackDispatcher;
import com.librax.lab.module.task.enums.TaskStatusEnum;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTimeoutWatchdog {

    private final TaskMapper taskMapper;
    private final TaskRouter taskRouter;
    private final TaskCallbackDispatcher callbackDispatcher;
    private final TaskEventPublisher eventPublisher;

    // TODO: 从 lab_task_executor_config 表加载各类型超时配置
    private static final Map<String, Long> TIMEOUT_MS_MAP = Map.of(
            TaskTypeEnum.INSTRUMENT.name(), 60_000L,
            TaskTypeEnum.AGV.name(),        120_000L,
            TaskTypeEnum.COMPUTE.name(),    10_000L,
            TaskTypeEnum.MANUAL.name(),     86_400_000L,
            TaskTypeEnum.NOTIFY.name(),     10_000L
    );
    private static final Map<String, Long> BACKOFF_MS_MAP = Map.of(
            TaskTypeEnum.AGV.name(), 5_000L
    );
    private static final long DEFAULT_TIMEOUT_MS = 60_000L;
    private static final long DEFAULT_BACKOFF_MS  = 2_000L;

    /** 每 15 秒扫描 ASSIGNED/EXECUTING 状态的超时任务 */
    @Scheduled(fixedDelay = 15_000, initialDelay = 30_000)
    public void scanTaskTimeout() {
        List<TaskDO> activeTasks = taskMapper.selectActiveTasks();
        LocalDateTime now = LocalDateTime.now();
        for (TaskDO task : activeTasks) {
            try {
                checkTimeout(task, now);
            } catch (Exception e) {
                log.error("[TaskTimeoutWatchdog] 检查超时异常 taskId={}",
                        task.getTaskId(), e);
            }
        }
    }

    /** 每分钟扫描超过 deadline 的 MANUAL 任务 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void scanManualDeadline() {
        List<TaskDO> overdue = taskMapper.selectOverdueManualTasks(LocalDateTime.now());
        for (TaskDO task : overdue) {
            log.warn("[TaskTimeoutWatchdog] 人工任务超期 taskId={} deadline={}",
                    task.getTaskId(), task.getDeadlineAt());
            handleTimeout(task, "MANUAL_DEADLINE", "人工任务超过截止时间");
        }
    }

    private void checkTimeout(TaskDO task, LocalDateTime now) {
        // 根据状态选择计时起点：ASSIGNED 从分配时算，EXECUTING 从开始执行时算
        LocalDateTime startPoint = TaskStatusEnum.ASSIGNED.name().equals(task.getStatus())
                ? task.getAssignedAt()
                : task.getStartedAt();
        if (startPoint == null) return;

        long timeoutMs  = TIMEOUT_MS_MAP.getOrDefault(task.getTaskType(), DEFAULT_TIMEOUT_MS);
        long elapsedMs  = Duration.between(startPoint, now).toMillis();
        if (elapsedMs <= timeoutMs) return;

        log.warn("[TaskTimeoutWatchdog] 任务超时 taskId={} type={} status={} " +
                        "elapsed={}ms timeout={}ms",
                task.getTaskId(), task.getTaskType(),
                task.getStatus(), elapsedMs, timeoutMs);

        handleTimeout(task, "TASK_TIMEOUT",
                String.format("任务超时: 已执行%dms 阈值%dms", elapsedMs, timeoutMs));
    }

    private void handleTimeout(TaskDO task, String errorCode, String errorMsg) {
        // CAS 防止和正常回调并发
        int rows = taskMapper.casStatusToFailed(
                task.getTaskId(), task.getStatus(), errorCode, errorMsg);
        if (rows == 0) {
            log.debug("[TaskTimeoutWatchdog] 状态已变更，跳过 taskId={}", task.getTaskId());
            return;
        }

        eventPublisher.publish(task.getTaskId(), "TIMEOUT",
                task.getStatus(), TaskStatusEnum.FAILED.name(),
                Map.of("errorCode", errorCode, "errorMsg", errorMsg));

        // 判断是否还有重试次数
        if (task.getRetryCount() < task.getMaxRetry()) {
            log.info("[TaskTimeoutWatchdog] 安排重试 taskId={} retryCount={}/{}",
                    task.getTaskId(), task.getRetryCount() + 1, task.getMaxRetry());

            taskMapper.incrementRetryAndReset(task.getTaskId());
            eventPublisher.publish(task.getTaskId(), "RETRYING",
                    TaskStatusEnum.FAILED.name(), TaskStatusEnum.PENDING.name(),
                    Map.of("retryCount", task.getRetryCount() + 1));

            long backoffMs = BACKOFF_MS_MAP.getOrDefault(
                    task.getTaskType(), DEFAULT_BACKOFF_MS);
            taskRouter.enqueueWithDelay(task, backoffMs);

        } else {
            // 重试耗尽，上报失败给引擎
            log.warn("[TaskTimeoutWatchdog] 重试耗尽，上报失败 taskId={} retryCount={}/{}",
                    task.getTaskId(), task.getRetryCount(), task.getMaxRetry());
            callbackDispatcher.dispatch(
                    task.getCallbackToken(),
                    false, null, errorCode, errorMsg);
        }
    }
}