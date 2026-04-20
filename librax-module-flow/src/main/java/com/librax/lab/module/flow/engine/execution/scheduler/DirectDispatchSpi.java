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

import static com.librax.lab.module.flow.engine.execution.scheduler.SchedulerConstants.*;

/**
 * 直连分发 SPI 实现
 *
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
        // TODO: 迁到 device 模块后，在此处插入资源申请
        //       AcquireResult result = resourcePool.acquire(
        //           ctx.getDeviceType(), ctx.getZoneCode(),
        //           ctx.getExecutionId() + ":" + ctx.getNodeId());
        //       if (!result.isSuccess()) { return; } // 资源不可用，等下一轮调度


        // 选执行器（降级到 Mock）
        StepExecutor executor = executorFactory.hasExecutor(
                StepTypeEnum.valueOf(ctx.getStepType()))
                ? executorFactory.getExecutor(StepTypeEnum.valueOf(ctx.getStepType()))
                : mockStepExecutor;

        try {
            StepResult result = executor.execute(ctx);

            if (result.isWaiting()) {
                handleWaiting(ctx, result);
                return;
            }

            // TODO: 同步完成后在此处释放资源
            //       resourcePool.release(resourceId, ctx.getZoneCode(), ctx.getDeviceType());

            ctx.getCallback().onComplete(
                    ctx.getExecutionId(), ctx.getNodeId(), ctx.getAttempt(),
                    result.isSuccess(), result.getOutputs(),
                    result.getErrorCode(), result.getErrorMsg());

        } catch (Exception e) {
            log.error("[DirectDispatchSpi] 执行异常 executionId={} nodeId={} error={}",
                    ctx.getExecutionId(), ctx.getNodeId(), e.getMessage(), e);
            // TODO: 异常时释放资源
            ctx.getCallback().onComplete(
                    ctx.getExecutionId(), ctx.getNodeId(), ctx.getAttempt(),
                    false, null, "EXECUTE_EXCEPTION", e.getMessage());
        }
    }

    private void handleWaiting(StepDispatchContext ctx, StepResult result) {
        String callbackToken = ctx.getCallbackToken();

        stepStateMachine.markWaiting(
                ctx.getExecutionId(), ctx.getNodeId(), ctx.getAttempt(),
                result.getWaitingFor(), callbackToken);

        Map<String, Object> waitingInfo = new HashMap<>();
        if (result.getOutputs() != null) {
            waitingInfo.putAll(result.getOutputs());
        }
        waitingInfo.put(WAITING_KEY_CALLBACK_TOKEN, callbackToken);
        waitingInfo.put(WAITING_KEY_WAITING_FOR,    result.getWaitingFor().name());
        waitingInfo.put(WAITING_KEY_WAITING_SINCE,  LocalDateTime.now().toString());

        // TODO: 把 resourceId 存入 waitingInfo，回调时用于释放资源
        //       waitingInfo.put("_resourceId", resourceId);

        log.info("[DirectDispatchSpi] 步骤进入等待 executionId={} nodeId={} " +
                        "waitingFor={} token={}",
                ctx.getExecutionId(), ctx.getNodeId(),
                result.getWaitingFor(), callbackToken);
    }
}