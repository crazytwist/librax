
package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.enums.StepTypeEnum;
import lombok.extern.slf4j.Slf4j;
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
 * <p>自动注册机制：Spring 启动时会将所有 {@link StepExecutor} 实现类注入进来，
 * 工厂通过 {@link StepExecutor#supportType()} 自动建立类型到执行器的映射关系。
 * 新增执行器只需实现接口并加 {@code @Component}，无需修改工厂代码。
 *
 * <p>执行器优先级：当 {@code run_mode=MOCK} 时，调度器会直接使用 {@link MockStepExecutor}，
 * 不经过工厂路由（开发测试阶段绕过真实设备）。
 */
@Slf4j
@Component
public class StepExecutorFactory {

    /**
     * 类型 → 执行器映射表
     * key: StepTypeEnum，value: 对应的执行器实现
     */
    private final Map<StepTypeEnum, StepExecutor> executorMap;

    /**
     * 构造时自动注入所有 StepExecutor 实现，建立映射关系
     *
     * @param executors Spring 自动发现的所有 StepExecutor 实现列表
     */
    public StepExecutorFactory(List<StepExecutor> executors) {
        this.executorMap = executors.stream()
                .filter(e -> e.supportType() != null)
                .collect(Collectors.toMap(
                        StepExecutor::supportType,
                        Function.identity(),
                        // 同一类型有多个实现时，后注册的覆盖先注册的（用于覆盖默认实现）
                        (existing, replacement) -> {
                            log.warn("[StepExecutorFactory] 执行器类型冲突，使用后注册的实现: type={} old={} new={}",
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
     * 根据步骤类型获取执行器
     *
     * @param stepType 步骤类型
     * @return 对应的执行器实现
     * @throws IllegalArgumentException 当没有注册该类型的执行器时抛出
     */
    public StepExecutor getExecutor(StepTypeEnum stepType) {
        StepExecutor executor = executorMap.get(stepType);
        if (executor == null) {
            throw new IllegalArgumentException(
                    "[StepExecutorFactory] 未找到执行器，请检查是否已实现并注册: stepType=" + stepType);
        }
        return executor;
    }

    /**
     * 判断某类型是否有注册的执行器
     *
     * @param stepType 步骤类型
     * @return true=有注册，false=未注册
     */
    public boolean hasExecutor(StepTypeEnum stepType) {
        return executorMap.containsKey(stepType);
    }
}