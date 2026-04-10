package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.device.gateway.DeviceGateway;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

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
    public StepResult execute(StepNode node, String executionId, Map<String, Object> inputParams) {

        // 1. 先生成 token
        String callbackToken = UUID.randomUUID().toString().replace("-", "");

        String deviceType = node.getDeviceType();
        String command = node.getCommand();

        log.info("[InstrumentExecutor] 发送设备指令 nodeId={} device={} cmd={}",
                node.getNodeId(), deviceType, command);

        // 调设备网关发指令（异步，设备完成后回调 /app-api/flow/callback/step-complete）
        String taskId = deviceGateway.sendCommand(deviceType, command, inputParams
                , executionId, node.getNodeId(), callbackToken);

        // ★ 返回 WAITING，不阻塞，等设备回调
        return StepResult.waitForDevice(Map.of(
                "deviceTaskId", taskId,
                "deviceType", deviceType,
                "command", command,
                "_callbackToken", callbackToken
        ));
    }
}
