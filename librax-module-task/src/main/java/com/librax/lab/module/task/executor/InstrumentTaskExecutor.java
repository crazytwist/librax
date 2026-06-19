package com.librax.lab.module.task.executor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.dispatch.TaskCallbackDispatcher;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 仪器任务执行器（QUEUED 路径）
 *
 * <p>职责：从 {@code lab_task.payload} 解析设备指令参数，
 * 通过 {@link DeviceCommandSpi} 向目标设备发送指令，然后根据指令的完成模式决定后续处理：
 * <ul>
 *   <li><b>SYNC 模式</b>（{@code DeviceCommandSpi.Result#isSyncCompleted()} 为 true）：
 *       HTTP 响应即完成，立即调用 {@link TaskCallbackDispatcher} 关闭任务并推进 DAG。</li>
 *   <li><b>ASYNC 模式</b>（WEBHOOK/POLL）：发完指令后将 task 状态更新为 EXECUTING 并返回。
 *       设备完成后通过 webhook 回调 {@code DeviceCallbackController}，
 *       由 {@code DeviceCallbackHandler} 推进 DAG 并通过 {@code TaskCallbackSpi} 关闭任务。</li>
 * </ul>
 *
 * <p>Payload 字段约定（由 {@code QueuedDispatchSpi.buildPayload()} 填充）：
 * <pre>
 * {
 *   "deviceType":  "PH_METER",
 *   "commandCode": "MEASURE",
 *   "inputParams": { ... }
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstrumentTaskExecutor implements TaskExecutor {

    private final TaskMapper              taskMapper;
    private final TaskCallbackDispatcher  callbackDispatcher;
    private final DeviceCommandSpi        deviceCommandSpi;

    @Override
    public String supportType() {
        return TaskTypeEnum.INSTRUMENT.name();
    }

    @Override
    public void execute(TaskDO task) {
        log.info("[InstrumentTaskExecutor] 开始执行仪器任务 taskId={} executionId={} nodeId={}",
                task.getTaskId(), task.getExecutionId(), task.getNodeId());

        // 1. 解析 payload
        String deviceType;
        String commandCode;
        Map<String, Object> inputParams;
        try {
            Map<String, Object> payload = JSON.parseObject(
                    task.getPayload(), new TypeReference<Map<String, Object>>() {});
            deviceType   = (String) payload.get("deviceType");
            commandCode  = (String) payload.get("commandCode");
            // inputParams 存储为 JSON 子对象
            Object rawParams = payload.get("inputParams");
            if (rawParams instanceof Map) {
                //noinspection unchecked
                inputParams = (Map<String, Object>) rawParams;
            } else {
                inputParams = Map.of();
            }
        } catch (Exception e) {
            log.error("[InstrumentTaskExecutor] payload 解析失败 taskId={} payload={}",
                    task.getTaskId(), task.getPayload(), e);
            callbackDispatcher.dispatch(
                    task.getCallbackToken(), false, null,
                    "PAYLOAD_PARSE_ERROR", "指令参数解析失败: " + e.getMessage());
            return;
        }

        if (deviceType == null || deviceType.isBlank()
                || commandCode == null || commandCode.isBlank()) {
            log.error("[InstrumentTaskExecutor] payload 缺少 deviceType/commandCode taskId={}",
                    task.getTaskId());
            callbackDispatcher.dispatch(
                    task.getCallbackToken(), false, null,
                    "MISSING_DEVICE_PARAMS", "deviceType 或 commandCode 为空");
            return;
        }

        // 2. 发送设备指令
        DeviceCommandSpi.Result result;
        try {
            result = deviceCommandSpi.send(
                    deviceType, commandCode, inputParams,
                    task.getExecutionId(), task.getNodeId(),
                    task.getCallbackToken());
        } catch (Exception e) {
            log.error("[InstrumentTaskExecutor] 指令发送失败 taskId={} deviceType={} commandCode={}",
                    task.getTaskId(), deviceType, commandCode, e);
            callbackDispatcher.dispatch(
                    task.getCallbackToken(), false, null,
                    "DEVICE_SEND_FAILED", "设备指令发送失败: " + e.getMessage());
            return;
        }

        // 3. 根据完成模式决定后续处理
        if (result.isSyncCompleted()) {
            // SYNC 模式：HTTP 响应即完成，立即回调成功
            log.info("[InstrumentTaskExecutor] 同步完成 taskId={} externalTaskId={}",
                    task.getTaskId(), result.getTaskId());
            Map<String, Object> outputs = Map.of(
                    "deviceTaskId", result.getTaskId(),
                    "deviceType",   deviceType,
                    "command",      commandCode
            );
            callbackDispatcher.dispatch(task.getCallbackToken(), true, outputs, null, null);
        } else {
            // ASYNC 模式（WEBHOOK/POLL）：更新任务为 EXECUTING，等设备回调
            // 设备完成后 → DeviceCallbackHandler → StepCallbackSpi（推进 DAG）+ TaskCallbackSpi（关闭 task）
            log.info("[InstrumentTaskExecutor] 异步模式，等待设备回调 taskId={} externalTaskId={}",
                    task.getTaskId(), result.getTaskId());
            taskMapper.updateExecuting(task.getTaskId(), result.getTaskId());
        }
    }

    @Override
    public void cancel(TaskDO task) {
        log.info("[InstrumentTaskExecutor] 取消仪器任务 taskId={}", task.getTaskId());
        // 取消逻辑由 DeviceGateway.cancel() 处理，此处仅记录日志
        // 实际取消通过 TaskService.cancelTask() → DeviceGateway.cancel() 实现
    }
}
