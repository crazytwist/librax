package com.librax.lab.module.task.executor;

import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.dataobject.taskexecutorconfig.TaskExecutorConfigDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.dal.mysql.taskexecutorconfig.TaskExecutorConfigMapper;
import com.librax.lab.module.task.dispatch.TaskCallbackDispatcher;
import com.librax.lab.module.task.enums.TaskStatusEnum;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final TaskExecutorConfigMapper executorConfigMapper;

    // 各类型任务超时兜底值（DB 无配置时使用）
    // MANUAL 类型不在此处配置超时，由 scanManualDeadline 按业务 deadline 处理
    private static final Map<String, Long> DEFAULT_TIMEOUT_MS_MAP = Map.of(
            TaskTypeEnum.INSTRUMENT.name(), 60_000L,
            TaskTypeEnum.AGV.name(),        120_000L,
            TaskTypeEnum.COMPUTE.name(),    10_000L,
            TaskTypeEnum.NOTIFY.name(),     10_000L
    );
    private static final Map<String, Long> DEFAULT_BACKOFF_MS_MAP = Map.of(
            TaskTypeEnum.AGV.name(), 5_000L
    );
    private static final long GLOBAL_DEFAULT_TIMEOUT_MS = 60_000L;
    private static final long GLOBAL_DEFAULT_BACKOFF_MS  = 2_000L;

    // 运行时从 DB 加载，覆盖上面的兜底值
    private volatile Map<String, Long> timeoutMsMap  = DEFAULT_TIMEOUT_MS_MAP;
    private volatile Map<String, Long> backoffMsMap  = DEFAULT_BACKOFF_MS_MAP;

    @PostConstruct
    public void loadExecutorConfigs() {
        try {
            List<TaskExecutorConfigDO> configs = executorConfigMapper.selectAllEnabled();
            Map<String, Long> timeout = new HashMap<>(DEFAULT_TIMEOUT_MS_MAP);
            Map<String, Long> backoff = new HashMap<>(DEFAULT_BACKOFF_MS_MAP);
            for (TaskExecutorConfigDO cfg : configs) {
                if (cfg.getTaskTimeoutMs() != null && cfg.getTaskTimeoutMs() > 0) {
                    timeout.put(cfg.getExecutorType(), cfg.getTaskTimeoutMs());
                }
                if (cfg.getRetryBackoffMs() != null && cfg.getRetryBackoffMs() > 0) {
                    backoff.put(cfg.getExecutorType(), cfg.getRetryBackoffMs());
                }
            }
            this.timeoutMsMap = Map.copyOf(timeout);
            this.backoffMsMap = Map.copyOf(backoff);
            log.info("[TaskTimeoutWatchdog] 执行器超时配置已加载 configs={}", configs.size());
        } catch (Exception e) {
            log.error("[TaskTimeoutWatchdog] 加载执行器配置失败，使用兜底值", e);
        }
    }

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

    /**
     * 每 30 秒扫描在队列中等待过久的 PENDING 任务
     * <p>
     * 场景：任务已入队（PENDING）但执行器饱和，长时间未被分配（ASSIGNED）。
     * 使用与执行超时相同的 timeoutMsMap：若任务类型未配置超时，则不做队列等待超时检查。
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 45_000)
    public void scanQueueWaitTimeout() {
        List<TaskDO> pendingTasks = taskMapper.selectPendingTasks();
        LocalDateTime now = LocalDateTime.now();
        for (TaskDO task : pendingTasks) {
            try {
                checkQueueWaitTimeout(task, now);
            } catch (Exception e) {
                log.error("[TaskTimeoutWatchdog] 队列等待超时检查异常 taskId={}",
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
        // MANUAL 任务不设执行超时，由 scanManualDeadline 按 deadline 字段处理
        if (TaskTypeEnum.MANUAL.name().equals(task.getTaskType())) return;

        // 根据状态选择计时起点：ASSIGNED 从分配时算，EXECUTING 从开始执行时算
        LocalDateTime startPoint = TaskStatusEnum.ASSIGNED.name().equals(task.getStatus())
                ? task.getAssignedAt()
                : task.getStartedAt();
        if (startPoint == null) return;

        long timeoutMs  = timeoutMsMap.getOrDefault(task.getTaskType(), GLOBAL_DEFAULT_TIMEOUT_MS);
        long elapsedMs  = Duration.between(startPoint, now).toMillis();
        if (elapsedMs <= timeoutMs) return;

        log.warn("[TaskTimeoutWatchdog] 任务超时 taskId={} type={} status={} " +
                        "elapsed={}ms timeout={}ms",
                task.getTaskId(), task.getTaskType(),
                task.getStatus(), elapsedMs, timeoutMs);

        handleTimeout(task, "TASK_TIMEOUT",
                String.format("任务超时: 已执行%dms 阈值%dms", elapsedMs, timeoutMs));
    }

    private void checkQueueWaitTimeout(TaskDO task, LocalDateTime now) {
        // MANUAL 任务无队列等待超时
        if (TaskTypeEnum.MANUAL.name().equals(task.getTaskType())) return;

        // 未配置超时的任务类型，队列等待也不超时
        Long timeoutMs = timeoutMsMap.get(task.getTaskType());
        if (timeoutMs == null) return;

        long waitedMs = Duration.between(task.getQueuedAt(), now).toMillis();
        if (waitedMs <= timeoutMs) return;

        log.warn("[TaskTimeoutWatchdog] 任务队列等待超时 taskId={} type={} waited={}ms timeout={}ms",
                task.getTaskId(), task.getTaskType(), waitedMs, timeoutMs);

        handleTimeout(task, "TASK_QUEUE_TIMEOUT",
                String.format("任务在队列中等待过久: 已等待%dms 阈值%dms", waitedMs, timeoutMs));
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

            long backoffMs = backoffMsMap.getOrDefault(
                    task.getTaskType(), GLOBAL_DEFAULT_BACKOFF_MS);
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