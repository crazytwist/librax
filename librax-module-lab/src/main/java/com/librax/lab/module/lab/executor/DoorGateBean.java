package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开关门网关 Bean。同一个 Bean 复用两次：一次开门（op=open），一次关门（op=close），
 * 具体 op 值由 {@code pd_pipeline_step.params_override} 在节点级别写死，见
 * {@code sql/add_door_gate_step.sql} 的 s_door_open / s_door_close 两个节点。
 *
 * <p>是否需要走开关门由流程入参 {@code doorRequired} 决定（来自 AGV 路由配置
 * {@code AgvRouteConfigDO.doorRequired}）：
 * <ul>
 *   <li>{@code doorRequired=false}（或未传）：立即返回成功，不下发任何设备指令；</li>
 *   <li>{@code doorRequired=true}：向 {@code doorDeviceId} 设备下发 {@code doorCommandCode}
 *       指令，带上本节点的 {@code op}。目前对接的 Feeding-Reaction 工站 OpenCloseDoor
 *       接口是 completion_mode=SYNC，HTTP 响应即代表开/关门完成，不需要等设备回调；
 *       如果换成异步门禁设备，返回 syncCompleted=false 时会自动挂起等通用设备回调。</li>
 * </ul>
 *
 * <p>输入参数（{@code ctx.getInputParams()}）：
 * <pre>
 *   doorRequired          是否需要开关门，布尔（字符串 "true"/"false" 亦可）
 *   op                    "open" 或 "close"，节点级别写死，不来自补料单
 *   doorDeviceId          门禁设备类型，默认 Feeding-Reaction
 *   doorCommandCode       开关门指令码，默认 OpenCloseDoor
 *   doorCallbackBaseUrl   仅异步设备需要：门禁回调用的服务基础地址，
 *                         实际回调地址为 {base}/app-api/device/callback/{executionId}/{nodeId}
 * </pre>
 */
@Slf4j
@Component("doorGateBean")
@RequiredArgsConstructor
public class DoorGateBean implements StepExecutor {

    private static final String DEFAULT_DOOR_DEVICE_TYPE = "Feeding-Reaction";
    private static final String DEFAULT_DOOR_COMMAND_CODE = "OpenCloseDoor";
    private static final String DEFAULT_OP = "open";

    private final DeviceCommandSpi deviceCommandSpi;

    @Override
    public StepTypeEnum supportType() {
        return null; // 仅按 beanName 路由
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        Map<String, Object> inputParams = ctx.getInputParams();
        String op = string(inputParams.get("op"), DEFAULT_OP);

        if (!bool(inputParams.get("doorRequired"))) {
            log.info("[DoorGate] 本次不需要开关门 op={} executionId={} nodeId={}",
                    op, ctx.getExecutionId(), ctx.getNodeId());
            return StepResult.ok(Map.of("doorOp", op, "executed", false));
        }

        String deviceType = string(inputParams.get("doorDeviceId"), DEFAULT_DOOR_DEVICE_TYPE);
        String commandCode = string(inputParams.get("doorCommandCode"), DEFAULT_DOOR_COMMAND_CODE);
        String callbackBaseUrl = string(inputParams.get("doorCallbackBaseUrl"), null);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("op", op);
        if (callbackBaseUrl != null) {
            params.put("callbackUrl", callbackBaseUrl + "/app-api/device/callback/"
                    + ctx.getExecutionId() + "/" + ctx.getNodeId());
        }

        DeviceCommandSpi.Result result = deviceCommandSpi.send(
                deviceType, commandCode, params,
                ctx.getExecutionId(), ctx.getNodeId(), ctx.getCallbackToken());

        if (result.isSyncCompleted()) {
            // Feeding-Reaction 的 OpenCloseDoor 是 SYNC：HTTP 响应即代表开/关门完成，无需等回调
            log.info("[DoorGate] 开关门同步完成 op={} executionId={} nodeId={} deviceType={} commandCode={}",
                    op, ctx.getExecutionId(), ctx.getNodeId(), deviceType, commandCode);
            return StepResult.ok(Map.of("doorOp", op, "executed", true, "deviceTaskId", result.getTaskId()));
        }

        // 兜底：如果这个设备类型以后换成异步门禁，走通用设备回调挂起
        log.info("[DoorGate] 已下发开关门指令，等待现场完成 op={} executionId={} nodeId={} deviceType={} commandCode={}",
                op, ctx.getExecutionId(), ctx.getNodeId(), deviceType, commandCode);
        return StepResult.waitForDevice(Map.of(
                "waitReason", "等待现场开关门完成",
                "op", op,
                "deviceType", deviceType,
                "commandCode", commandCode));
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean b) return b;
        return value != null && "true".equalsIgnoreCase(String.valueOf(value));
    }

    private String string(Object value, String defaultValue) {
        if (value == null || value.toString().isBlank()) return defaultValue;
        return value.toString();
    }
}
