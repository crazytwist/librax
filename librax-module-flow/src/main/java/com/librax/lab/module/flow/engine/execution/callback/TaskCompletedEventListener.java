package com.librax.lab.module.flow.engine.execution.callback;

import com.librax.lab.module.flow.api.event.TaskCompletedEvent;
import com.librax.lab.module.infra.mdc.ExecutionMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 任务完成事件监听器 — 跨模块事件桥
 * <p>
 * 监听 task 模块发出的 {@link TaskCompletedEvent},调用本模块的
 * {@link StepCallbackService} 推进 DAG 调度。
 * <p>
 * 通过 Spring Event 机制,task 模块不再直接依赖 flow 模块的任何实现类,
 * 打破 task → flow → ... → task 的循环依赖。
 * <p>
 * {@code @Async} 避免阻塞 task 模块的回调线程,同时让事件处理逻辑
 * 脱离 task 侧的事务上下文,StepCallbackService 内部可以开新事务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCompletedEventListener {

    private final StepCallbackService stepCallbackService;

    @Async("labEventListenerExecutor")
    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        ExecutionMdc.set(event.getExecutionId(), event.getNodeId(), 0);
        try {
            log.info("[TaskCompletedListener] 收到任务完成事件 executionId={} nodeId={} success={}",
                    event.getExecutionId(), event.getNodeId(), event.isSuccess());
            stepCallbackService.callback(
                    event.getExecutionId(),
                    event.getNodeId(),
                    event.getCallbackToken(),
                    event.isSuccess(),
                    event.getOutputs(),
                    event.getErrorCode(),
                    event.getErrorMsg());
        } catch (Exception e) {
            log.error("[TaskCompletedListener] 推进调度异常 executionId={} nodeId={}",
                    event.getExecutionId(), event.getNodeId(), e);
        } finally {
            ExecutionMdc.clear();
        }
    }
}