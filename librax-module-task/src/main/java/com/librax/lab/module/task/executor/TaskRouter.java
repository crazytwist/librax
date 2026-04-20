package com.librax.lab.module.task.executor;

import com.librax.lab.module.infra.framework.util.LabIdGenerator;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.dispatch.TaskCallbackDispatcher;
import com.librax.lab.module.task.enums.TaskStatusEnum;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskRouter {


    private final TaskMapper taskMapper;
    private final TaskExecutorFactory executorFactory;
    private final TaskEventPublisher eventPublisher;
    private final TaskCallbackDispatcher callbackDispatcher;
    private final LabIdGenerator idGenerator;

    // key=taskType，value=有界队列（存 taskId）
    private final Map<String, BlockingQueue<String>> typeQueues = new ConcurrentHashMap<>();
    // key=taskType，value=并发控制信号量
    private final Map<String, Semaphore> typeSemaphores = new ConcurrentHashMap<>();

    // 默认配置（后续从 lab_task_executor_config 表加载）
    private static final int DEFAULT_QUEUE_CAPACITY = 500;
    private static final int DEFAULT_MAX_CONCURRENT = 10;

    // 每种类型一个单线程消费，用 ThreadFactory 命名便于排查
    private final ThreadFactory namedThreadFactory = r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    };

    @PostConstruct
    public void init() {
        // TODO: 从 lab_task_executor_config 表动态加载各类型配置
        //       目前使用默认值初始化所有已知类型
        for (TaskTypeEnum type : TaskTypeEnum.values()) {
            typeQueues.put(type.name(),
                    new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY));
            typeSemaphores.put(type.name(),
                    new Semaphore(DEFAULT_MAX_CONCURRENT));
        }

        // 每种类型启动一个守护线程做消费循环
        typeQueues.forEach((type, queue) -> {
            Thread t = new Thread(() -> consumeLoop(type, queue),
                    "task-router-" + type.toLowerCase());
            t.setDaemon(true);
            t.start();
        });

        log.info("[TaskRouter] 初始化完成 队列类型={}", typeQueues.keySet());
    }

    // ----------------------------------------------------------------
    // 入队
    // ----------------------------------------------------------------

    /**
     * 任务立即入队
     *
     * @param task 已持久化的任务记录
     */
    public void enqueue(TaskDO task) {
        doEnqueue(task, 0);
    }

    /**
     * 任务延迟入队（重试退避时使用）
     *
     * @param task    任务记录
     * @param delayMs 延迟毫秒数
     */
    public void enqueueWithDelay(TaskDO task, long delayMs) {
        if (delayMs <= 0) {
            doEnqueue(task, 0);
            return;
        }
        // 虚拟线程做延迟，不占用调度线程
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                doEnqueue(task, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void doEnqueue(TaskDO task, int retryTimes) {
        String type = task.getTaskType();


        BlockingQueue<String> queue = typeQueues.get(type);

        if (queue == null) {
            log.error("[TaskRouter] 不支持的任务类型 taskId={} type={}",
                    task.getTaskId(), type);
            failTask(task, "UNSUPPORTED_TYPE", "不支持的任务类型: " + type);
            return;
        }

        boolean offered = queue.offer(task.getTaskId());
        if (!offered) {
            // 队列满，触发背压
            log.warn("[TaskRouter] 队列已满，拒绝入队 taskId={} type={}", task.getTaskId(), type);
            failTask(task, "QUEUE_FULL", "执行队列已满，请稍后重试");
            return;
        }

        taskMapper.updateQueued(task.getTaskId());
        eventPublisher.publish(task.getTaskId(), "CREATED", null,
                TaskStatusEnum.PENDING.name(), null);

        log.info("[TaskRouter] 任务入队 taskId={} type={} priority={}",
                task.getTaskId(), type, task.getPriority());
    }

    // ----------------------------------------------------------------
    // 消费循环
    // ----------------------------------------------------------------

    private void consumeLoop(String type, BlockingQueue<String> queue) {
        log.info("[TaskRouter] 消费线程启动 type={}", type);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String taskId = queue.take();
                Semaphore semaphore = typeSemaphores.get(type);
                semaphore.acquire();

                new Thread(() -> {
                    try {
                        dispatch(taskId, type);
                    } finally {
                        semaphore.release();
                    }
                }).start();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("[TaskRouter] 消费线程中断 type={}", type);
            } catch (Exception e) {
                log.error("[TaskRouter] 消费循环异常 type={}", type, e);
            }
        }
    }

    private void dispatch(String taskId, String type) {
        TaskDO task = taskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.warn("[TaskRouter] 任务不存在 taskId={}", taskId);
            return;
        }
        if (TaskStatusEnum.CANCELLED.name().equals(task.getStatus())) {
            log.info("[TaskRouter] 任务已取消，跳过 taskId={}", taskId);
            return;
        }

        // TODO: 在此处接入 ResourcePool 申请资源（resource 模块就绪后实现）
        //       AcquireResult result = resourcePool.acquire(
        //           extractResourceSubtype(task), task.getZoneCode(), task.getTaskId());
        //       if (!result.isSuccess()) {
        //           enqueueWithDelay(task, 2000L); // 资源不可用，退避重试
        //           return;
        //       }
        //       taskMapper.updateExecutorId(task.getTaskId(), result.getResourceId());

        try {
            taskMapper.updateAssigned(task.getTaskId());
            eventPublisher.publish(taskId, "ASSIGNED",
                    TaskStatusEnum.PENDING.name(),
                    TaskStatusEnum.ASSIGNED.name(), null);

            TaskExecutor executor = executorFactory.getExecutor(type);
            executor.execute(task);

        } catch (Exception e) {
            log.error("[TaskRouter] 任务分发异常 taskId={} error={}",
                    taskId, e.getMessage(), e);
            // TODO: 资源模块接入后，在此处释放已申请的资源
            failTask(task, "DISPATCH_ERROR", e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // 工具
    // ----------------------------------------------------------------

    private void failTask(TaskDO task, String errorCode, String errorMsg) {
        taskMapper.casStatusToFailed(
                task.getTaskId(), task.getStatus(), errorCode, errorMsg);
        eventPublisher.publish(task.getTaskId(), "FAILED",
                task.getStatus(), TaskStatusEnum.FAILED.name(),
                Map.of("errorCode", errorCode, "errorMsg", errorMsg));
        // 通知引擎步骤失败
        callbackDispatcher.dispatch(
                task.getCallbackToken(), false, null, errorCode, errorMsg);
    }
}