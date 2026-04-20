package com.librax.lab.module.task.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskExecutorFactory {

    private final Map<String, TaskExecutor> executorMap;

    public TaskExecutorFactory(List<TaskExecutor> executors) {
        this.executorMap = executors.stream()
                .collect(Collectors.toMap(
                        TaskExecutor::supportType,
                        Function.identity(),
                        (a, b) -> {
                            log.warn("[TaskExecutorFactory] 类型冲突，使用后注册: {}",
                                    b.getClass().getSimpleName());
                            return b;
                        }));
        log.info("[TaskExecutorFactory] 已注册执行器: {}", executorMap.keySet());
    }

    public TaskExecutor getExecutor(String taskType) {
        TaskExecutor executor = executorMap.get(taskType);
        if (executor == null) {
            throw new IllegalArgumentException("未找到任务执行器: taskType=" + taskType);
        }
        return executor;
    }

    public boolean hasExecutor(String taskType) {
        return executorMap.containsKey(taskType);
    }
}
