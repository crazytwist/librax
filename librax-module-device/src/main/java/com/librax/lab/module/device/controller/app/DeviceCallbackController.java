package com.librax.lab.module.device.controller.app;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.device.callback.DeviceCallbackHandler;
import com.librax.lab.module.device.controller.app.vo.DeviceCallbackReqVO;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import com.librax.lab.module.device.service.devicedirectexec.DeviceDirectExecService;
import com.librax.lab.module.device.service.devicedirectexec.DeviceDirectExecServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

/**
 * 设备回调端点
 *
 * <p>设备完成任务后，通过 webhook 方式调用此接口通知系统。
 *
 * <ul>
 *   <li>流水线步骤回调：POST /app-api/device/callback/{executionId}/{nodeId}</li>
 *   <li>直接执行回调：  POST /app-api/device/callback/{execId}/direct</li>
 * </ul>
 */
@Slf4j
@Tag(name = "设备回调")
@RestController
@RequestMapping("device/callback")
@Validated
@RequiredArgsConstructor
public class DeviceCallbackController {

    private final DeviceCallbackHandler   callbackHandler;
    private final DeviceDirectExecService directExecService;
    private final DeviceStateCache        stateCache;
    private final StringRedisTemplate     redisTemplate;

    private static final String DIRECT_REDIS_PREFIX = "device:direct:exec:";

    /**
     * 流水线步骤设备回调（通用端点）
     *
     * <p>executionId 以 {@code de-} 开头时，自动路由到直接执行回调处理；
     * 其余情况推进流程 DAG。
     */
    @PostMapping("/{executionId}/{nodeId}")
    @PermitAll
    @Operation(summary = "设备完成回调（通用）",
            description = "设备通过 WEBHOOK 模式主动调用。" +
                    "executionId 以 de- 开头时等同于 /direct 端点，由 DeviceCallbackHandler 内部路由。")
    @Parameter(name = "executionId", description = "流程执行ID 或 直接执行ID（de- 前缀）")
    @Parameter(name = "nodeId",      description = "步骤节点ID 或 direct（直接执行固定值）")
    public CommonResult<Boolean> onDeviceCallback(
            @PathVariable("executionId") String executionId,
            @PathVariable("nodeId")      String nodeId,
            @RequestBody DeviceCallbackReqVO req) {
        callbackHandler.handle(executionId, nodeId, req);
        return success(true);
    }

    /**
     * 设备指令直接执行回调（专用端点）
     *
     * <p>对应 {@code DeviceDirectExecService.execute()} 触发的指令，设备完成后调用此接口。
     * 相比通用端点，此端点：
     * <ol>
     *   <li>明确语义，URL 中固定 nodeId=direct，无需猜测</li>
     *   <li>独立校验 callbackToken（防伪造回调）</li>
     *   <li>释放设备占用（markIdleByExecutionNode）</li>
     *   <li>将结果写入 Redis 执行记录，供 /execute/result 查询</li>
     * </ol>
     *
     * <p>回调地址示例：
     * {@code POST http://your-server/app-api/device/callback/de-a1b2c3d4/direct}
     */
    @PostMapping("/{execId}/direct")
    @PermitAll
    @Operation(summary = "设备指令直接执行回调（专用端点）",
            description = "调用 /lab/device-command/execute 后，设备完成任务通过此端点回调。\n\n" +
                    "- execId 由 /execute 接口返回\n" +
                    "- callbackToken 由 /execute 接口返回，必须原样携带，否则拒绝\n" +
                    "- 回调成功后设备自动释放（BUSY → IDLE）")
    @Parameter(name = "execId", description = "直接执行ID，格式 de-{uuid}，由 /execute 接口返回")
    public CommonResult<Boolean> onDirectExecCallback(
            @PathVariable("execId") String execId,
            @RequestBody DeviceCallbackReqVO req) {

        log.info("[DirectExecCallback] 收到回调 execId={} success={}", execId, req.isSuccess());

        // 1. 校验 execId 格式
        if (!execId.startsWith(DeviceDirectExecServiceImpl.EXEC_ID_PREFIX)) {
            log.warn("[DirectExecCallback] execId 格式非法，应以 de- 开头: {}", execId);
            return success(false);
        }

        // 2. 校验 callbackToken
        String redisKey      = DIRECT_REDIS_PREFIX + execId;
        Object storedToken   = redisTemplate.opsForHash().get(redisKey, "callbackToken");
        String requestToken  = req.getCallbackToken();

        if (storedToken == null) {
            log.warn("[DirectExecCallback] 执行记录不存在或已过期 execId={}", execId);
            return success(false);
        }
        if (requestToken != null && !requestToken.isEmpty() && !storedToken.equals(requestToken)) {
            log.warn("[DirectExecCallback] callbackToken 不匹配 execId={} expected={} got={}",
                    execId, storedToken, requestToken);
            return success(false);
        }

        // 3. 释放设备占用
        stateCache.markIdleByExecutionNode(execId, DeviceDirectExecServiceImpl.NODE_ID);

        // 4. 解析输出数据并写入执行记录
        Map<String, Object> outputs = req.getData() != null ? req.getData() : Map.of();

        directExecService.onCallback(
                execId,
                req.isSuccess(),
                outputs,
                req.getErrorCode(),
                req.getErrorMsg(),
                req.getRawResponse());

        return success(true);
    }
}
