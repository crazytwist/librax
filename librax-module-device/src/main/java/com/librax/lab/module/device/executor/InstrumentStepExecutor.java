package com.librax.lab.module.device.executor;

import com.librax.lab.module.device.driver.DeviceSendResult;
import com.librax.lab.module.device.gateway.DeviceGateway;
import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.librax.lab.module.flow.api.scheduler.SchedulerConstants.CONTEXT_KEY_RESOURCE_ID;

/**
 * 仪器步骤执行器
 *
 * <p>职责：向设备网关发送指令，立即返回 WAITING 状态等待设备回调。
 * 设备选择由 {@link DeviceGateway} 内部的 {@code DeviceSelector} 负责，
 * 执行器只关心"发什么指令"，不关心"用哪台设备"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstrumentStepExecutor implements StepExecutor {

    private final DeviceGateway deviceGateway;

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.INSTRUMENT;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        String deviceType = ctx.getDeviceType();
        String commandCode = ctx.getCommandCode();

        String resourceId = (String) ctx.getInputParams().get(CONTEXT_KEY_RESOURCE_ID);

        log.info("[InstrumentExecutor] 发送设备指令 nodeId={} deviceId={} device={} cmd={}",
                ctx.getNodeId(), resourceId, deviceType, commandCode);

        DeviceSendResult result = deviceGateway.sendCommand(
                deviceType,
                commandCode,
                ctx.getInputParams(),
                ctx.getExecutionId(),
                ctx.getNodeId(),
                ctx.getCallbackToken());

        Map<String, Object> outputs = Map.of(
                "deviceTaskId", result.getTaskId(),
                "deviceType", deviceType,
                "command", commandCode
        );

        // SYNC 模式：HTTP 响应即完成，直接推进下一节点，无需等待回调
        if (result.isSyncCompleted()) {
            log.info("[InstrumentExecutor] 同步完成(SYNC) nodeId={} taskId={}", ctx.getNodeId(), result.getTaskId());
            return StepResult.ok(outputs);
        }

        // 异步模式（WEBHOOK/POLL）：步骤进入 WAITING，等设备回调
        return StepResult.waitForDevice(outputs);
    }
}