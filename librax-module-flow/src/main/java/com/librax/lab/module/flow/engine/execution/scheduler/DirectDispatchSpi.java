package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.api.dispatch.DispatchSpi;
import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.engine.execution.executor.MockStepExecutor;
import com.librax.lab.module.flow.engine.execution.executor.StepExecutorFactory;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.librax.lab.module.flow.api.scheduler.SchedulerConstants.*;

/**
 * 直连分发 SPI 实现
 *
 * <p>位于 flow 模块，是引擎调度的一部分，不迁移到其他模块。
 * 资源申请/释放统一在 StepSubmitter 中处理，此处不重复操作。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DirectDispatchSpi implements DispatchSpi {

    private final StepStateMachine stepStateMachine;
    private final StepExecutorFactory executorFactory;
    private final MockStepExecutor mockStepExecutor;

    @Override
    public String supportMode() {
        return "DIRECT";
    }

    @Override
    public void dispatch(StepDispatchContext ctx) {
        StepTypeEnum stepType = StepTypeEnum.valueOf(ctx.getStepType());

        // 选执行器：
        // 1. COMPUTE 类型按 beanName 从 Spring 容器取具体实现（如 waterQualityCalcBean / notifyBean）
        // 2. 其他类型按 stepType 路由（CONDITION / WAIT 等）
        // 3. 找不到时降级到 MockStepExecutor
        StepExecutor executor = resolveExecutor(stepType, ctx.getBeanName());

        try {
            StepResult result = executor.execute(ctx);

            if (result.isWaiting()) {
                handleWaiting(ctx, result);
                return;
            }

            ctx.getCallback().onComplete(
                    ctx.getExecutionId(), ctx.getNodeId(), ctx.getAttempt(),
                    result.isSuccess(), result.getOutputs(),
                    result.getErrorCode(), result.getErrorMsg());

        } catch (Exception e) {
            log.error("[DirectDispatchSpi] 执行异常 executionId={} nodeId={} error={}",
                    ctx.getExecutionId(), ctx.getNodeId(), e.getMessage(), e);
            ctx.getCallback().onComplete(
                    ctx.getExecutionId(), ctx.getNodeId(), ctx.getAttempt(),
                    false, null, "EXECUTE_EXCEPTION", e.getMessage());
        }
    }

    /**
     * 路由执行器
     *
     * <p>COMPUTE 类型优先按 beanName 路由，其他类型按 stepType 路由，
     * 均找不到时降级 Mock。
     */
    private StepExecutor resolveExecutor(StepTypeEnum stepType, String beanName) {
        // COMPUTE 类型：按 beanName 路由
        if (stepType == StepTypeEnum.COMPUTE) {
            if (beanName != null && !beanName.isEmpty()) {
                try {
                    StepExecutor executor = executorFactory.getExecutor(stepType, beanName);
                    log.debug("[DirectDispatchSpi] COMPUTE 按 beanName 路由 beanName={} executor={}",
                            beanName, executor.getClass().getSimpleName());
                    return executor;
                } catch (Exception e) {
                    log.warn("[DirectDispatchSpi] beanName 路由失败，降级 Mock beanName={} reason={}",
                            beanName, e.getMessage());
                    return mockStepExecutor;
                }
            }
            // beanName 为空时降级 Mock
            log.warn("[DirectDispatchSpi] COMPUTE 步骤未配置 beanName，降级 Mock");
            return mockStepExecutor;
        }

        // 其他类型：按 stepType 路由
        if (executorFactory.hasExecutor(stepType)) {
            return executorFactory.getExecutor(stepType);
        }

        // 找不到时降级 Mock
        log.warn("[DirectDispatchSpi] 未找到执行器，降级 Mock stepType={}", stepType);
        return mockStepExecutor;
    }

    private void handleWaiting(StepDispatchContext ctx, StepResult result) {
        String callbackToken = ctx.getCallbackToken();

        stepStateMachine.markWaiting(
                ctx.getExecutionId(), ctx.getNodeId(), ctx.getAttempt(),
                result.getWaitingFor(), callbackToken);

        log.info("[DirectDispatchSpi] 步骤进入等待 executionId={} nodeId={} waitingFor={} token={}",
                ctx.getExecutionId(), ctx.getNodeId(),
                result.getWaitingFor(), callbackToken);
    }
}