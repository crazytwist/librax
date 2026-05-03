
package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 步骤执行器工厂
 *
 * <p>职责：根据步骤类型（{@link StepTypeEnum}）返回对应的执行器实现。
 *
 * <p>自动注册机制：Spring 启动时会将所有 {@link com.librax.lab.module.flow.api.executor.StepExecutor} 实现类注入进来，
 * 工厂通过 {@link com.librax.lab.module.flow.api.executor.StepExecutor#supportType()} 自动建立类型到执行器的映射关系。
 * 新增执行器只需实现接口并加 {@code @Component}，无需修改工厂代码。
 *
 * <p>执行器优先级：当 {@code run_mode=MOCK} 时，调度器会直接使用 {@link MockStepExecutor}，
 * 不经过工厂路由（开发测试阶段绕过真实设备）。
 */
@Slf4j
@Component
public class StepExecutorFactory {

    private final Map<StepTypeEnum, StepExecutor> executorMap;
    private final ApplicationContext applicationContext;

    public StepExecutorFactory(List<StepExecutor> executors,
                               ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.executorMap = executors.stream()
                .filter(e -> e.supportType() != null)
                .collect(Collectors.toMap(
                        StepExecutor::supportType,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("[StepExecutorFactory] 执行器类型冲突 type={} old={} new={}",
                                    replacement.supportType(),
                                    existing.getClass().getSimpleName(),
                                    replacement.getClass().getSimpleName());
                            return replacement;
                        }
                ));

        log.info("[StepExecutorFactory] 已注册执行器: {}",
                executorMap.entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey().name(),
                                e -> e.getValue().getClass().getSimpleName())));
    }

    /**
     * 按步骤类型路由（CONDITION / WAIT / NOTIFY 等）
     */
    public StepExecutor getExecutor(StepTypeEnum stepType) {
        StepExecutor executor = executorMap.get(stepType);
        if (executor == null) {
            throw new IllegalArgumentException(
                    "[StepExecutorFactory] 未找到执行器: stepType=" + stepType);
        }
        return executor;
    }

    /**
     * COMPUTE 类型按 beanName 从 Spring 容器取具体实现
     * beanName 对应 pd_step_definition.bean_name
     */
    public StepExecutor getExecutor(StepTypeEnum stepType, String beanName) {
        // 只有 COMPUTE 类型才按 beanName 路由
        if (stepType == StepTypeEnum.COMPUTE && beanName != null && !beanName.isEmpty()) {
            try {
                return applicationContext.getBean(beanName, StepExecutor.class);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "[StepExecutorFactory] 未找到 COMPUTE 执行器: beanName=" + beanName, e);
            }
        }
        return getExecutor(stepType);
    }

    public boolean hasExecutor(StepTypeEnum stepType) {
        return executorMap.containsKey(stepType);
    }
}