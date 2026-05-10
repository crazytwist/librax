package com.librax.lab.module.device.controller.app;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.device.callback.DeviceCallbackHandler;
import com.librax.lab.module.device.controller.vo.DeviceCallbackReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

/**
 * 设备回调端点
 *
 * <p>设备完成任务后，通过 webhook 方式调用此接口通知系统。
 * 回调 URL 格式：{baseUrl}/app-api/device/callback/{executionId}/{nodeId}
 */
@Slf4j
@Tag(name = "设备回调")
@RestController
@RequestMapping("/app-api/device/callback")
@Validated
@RequiredArgsConstructor
public class DeviceCallbackController {

    private final DeviceCallbackHandler callbackHandler;

    @PostMapping("/{executionId}/{nodeId}")
    @Operation(summary = "设备完成回调")
    public CommonResult<Boolean> onDeviceCallback(
            @PathVariable("executionId") String executionId,
            @PathVariable("nodeId") String nodeId,
            @RequestBody DeviceCallbackReqVO req) {
        callbackHandler.handle(executionId, nodeId, req);
        return success(true);
    }
}
