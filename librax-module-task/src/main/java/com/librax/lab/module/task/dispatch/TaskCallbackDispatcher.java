package com.librax.lab.module.task.dispatch;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.api.event.TaskCompletedEvent;
import com.librax.lab.module.flow.api.resource.ResourcePool;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.enums.TaskStatusEnum;
import com.librax.lab.module.task.executor.TaskEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 任务完成回调统一入口
 * <p>
 * 所有外部系统(AGV 调度端、人工审批、计算任务同步完成、Watchdog 兜底)
 * 的任务完成回调都汇聚到这里,职责:
 * <ol>
 *   <li>幂等校验 — 任务已终态直接忽略重复回调</li>
 *   <li>CAS 更新任务终态 — 防止和 Watchdog 超时标记的并发冲突</li>
 *   <li>写 lab_task_event 审计日志</li>
 *   <li>发 {@link TaskCompletedEvent} 推进 flow 模块的 DAG</li>
 *   <li>TODO:resource 模块接入后,在此处释放资源</li>
 * </ol>
 * 不直接调 flow 模块的 StepCallbackService,通过 Spring Event 解耦。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCallbackDispatcher {

    private final TaskMapper taskMapper;
    private final TaskEventPublisher eventPublisher;
    private final ApplicationEventPublisher springEventPublisher;
    private final ResourcePool resourcePool;


    /**
     * 任务完成回调
     *
     * @param callbackToken 回调令牌(从 pe_step_execution 生成,透传全链路)
     * @param success       执行是否成功
     * @param outputs       任务产出(成功时写入 lab_task.result 并带给引擎)
     * @param errorCode     错误码(失败时)
     * @param errorMsg      错误信息(失败时)
     */
    public void dispatch(String callbackToken,
                         boolean success,
                         Map<String, Object> outputs,
                         String errorCode,
                         String errorMsg) {

        // 1. 根据 token 反查任务
        TaskDO task = taskMapper.selectByCallbackToken(callbackToken);
        if (task == null) {
            log.warn("[TaskCallbackDispatcher] 找不到任务 token={}", callbackToken);
            return;
        }

        // 2. 幂等:已终态直接跳过(防外部系统网络重试重复投递)
        String currentStatus = task.getStatus();
        if (TaskStatusEnum.valueOf(currentStatus).isTerminal()) {
            log.info("[TaskCallbackDispatcher] 任务已终态,跳过 taskId={} status={}",
                    task.getTaskId(), currentStatus);
            return;
        }

        log.info("[TaskCallbackDispatcher] 收到回调 taskId={} type={} success={}",
                task.getTaskId(), task.getTaskType(), success);

        // 3. CAS 更新任务终态 — 和 Watchdog 的超时 CAS 互斥,避免状态被覆盖
        int rows;
        if (success) {
            rows = taskMapper.casUpdateDone(
                    task.getTaskId(),
                    currentStatus,
                    outputs != null ? JSON.toJSONString(outputs) : null);
        } else {
            rows = taskMapper.casStatusToFailed(
                    task.getTaskId(), currentStatus, errorCode, errorMsg);
        }

        if (rows == 0) {
            // CAS 失败:任务状态已被其他线程改变(如 Watchdog 刚标为 FAILED)
            // 不发事件,避免 DAG 被重复推进
            log.warn("[TaskCallbackDispatcher] CAS 失败,状态已被其他线程改变 taskId={} expectedStatus={}",
                    task.getTaskId(), currentStatus);
            return;
        }

        // 4. 写事件日志
        String toStatus = success ? TaskStatusEnum.DONE.name() : TaskStatusEnum.FAILED.name();

        Map<String, Object> payload = success ? null
                : Map.of("errorCode", String.valueOf(errorCode),
                "errorMsg", String.valueOf(errorMsg));

        eventPublisher.publish(task.getTaskId(), toStatus, currentStatus, toStatus, payload);

        // 释放该步骤持有的所有资源(QUEUED 路径)
        String holderKey = task.getExecutionId() + ":" + task.getNodeId() + ":" + task.getAttempt();
        int released = resourcePool.releaseByHolder(holderKey);
        if (released > 0) {
            log.info("[TaskCallbackDispatcher] 释放资源 holder={} count={}", holderKey, released);
        }

        // 5. 发 Spring Event,flow 模块监听后推进 DAG 调度
        springEventPublisher.publishEvent(new TaskCompletedEvent(
                this,
                callbackToken,
                task.getExecutionId(),
                task.getNodeId(),
                success,
                outputs,
                errorCode,
                errorMsg));

        log.info("[TaskCallbackDispatcher] 已发布 TaskCompletedEvent taskId={} nodeId={}",
                task.getTaskId(), task.getNodeId());
    }
}