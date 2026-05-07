package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 步骤执行器工厂
 *
 * <p>职责：根据步骤类型（{@link StepTypeEnum}）返回对应的执行器实现。
 *
 * <p>注册机制：不在构造函数里收集执行器（避免循环依赖），
 * 改为第一次使用时从 ApplicationContext 懒加载。
 * 所有 {@link StepExecutor} 实现类加 {@code @Component} 即可自动被发现，无需修改工厂。
 *
 * <p>COMPUTE 类型执行器按 beanName 路由，其他类型按 stepType 路由。
 * supportType() 返回 null 的执行器只参与 beanName 路由，不进入 stepType 注册表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StepExecutorFactory {

    private final ApplicationContext applicationContext;

    // 懒加载，第一次调用时初始化，之后复用
    private volatile Map<StepTypeEnum, StepExecutor> executorMap;

    // ================================================================
    // 公开方法
    // ================================================================

    /**
     * 按步骤类型路由（CONDITION / WAIT / INSTRUMENT 等）
     */
    public StepExecutor getExecutor(StepTypeEnum stepType) {
        StepExecutor executor = getExecutorMap().get(stepType);
        if (executor == null) {
            throw new IllegalArgumentException(
                    "[StepExecutorFactory] 未找到执行器，请检查是否已实现并注册: stepType="
                            + stepType);
        }
        return executor;
    }

    /**
     * COMPUTE 类型按 beanName 从 Spring 容器取具体实现
     * beanName 对应 pd_step_definition.bean_name，如 waterQualityCalcBean / notifyBean
     */
    public StepExecutor getExecutor(StepTypeEnum stepType, String beanName) {
        if (stepType == StepTypeEnum.COMPUTE
                && beanName != null && !beanName.isEmpty()) {
            try {
                return applicationContext.getBean(beanName, StepExecutor.class);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "[StepExecutorFactory] 未找到 COMPUTE 执行器: beanName="
                                + beanName, e);
            }
        }
        return getExecutor(stepType);
    }

    /**
     * 判断某类型是否有注册的执行器
     */
    public boolean hasExecutor(StepTypeEnum stepType) {
        return getExecutorMap().containsKey(stepType);
    }

    // ================================================================
    // 私有方法
    // ================================================================

    /**
     * 懒加载执行器注册表
     * 双重检查锁，保证线程安全且只初始化一次
     */
    private Map<StepTypeEnum, StepExecutor> getExecutorMap() {
        if (executorMap == null) {
            synchronized (this) {
                if (executorMap == null) {
                    executorMap = applicationContext
                            .getBeansOfType(StepExecutor.class)
                            .values()
                            .stream()
                            .filter(e -> e.supportType() != null)
                            .collect(Collectors.toMap(
                                    StepExecutor::supportType,
                                    Function.identity(),
                                    (existing, replacement) -> {
                                        log.warn("[StepExecutorFactory] 执行器类型冲突，" +
                                                        "使用后注册的实现: type={} old={} new={}",
                                                replacement.supportType(),
                                                existing.getClass().getSimpleName(),
                                                replacement.getClass().getSimpleName());
                                        return replacement;
                                    }));

                    log.info("[StepExecutorFactory] 已注册执行器: {}",
                            executorMap.entrySet().stream()
                                    .collect(Collectors.toMap(
                                            e -> e.getKey().name(),
                                            e -> e.getValue().getClass().getSimpleName())));
                }
            }
        }
        return executorMap;
    }
}