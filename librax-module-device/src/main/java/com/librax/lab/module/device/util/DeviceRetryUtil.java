package com.librax.lab.module.device.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.librax.lab.module.device.driver.DeviceSendResult;
import com.librax.lab.module.device.exception.DeviceException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 设备指令发送结果重试工具
 *
 * <p>当发送指令返回的响应体 JSON 中包含指定业务码（如舱门未关闭 code=1217）时，
 * 视为可重试的瞬时失败，按固定间隔重试发送，直到成功或达到最大尝试次数。
 *
 * <pre>
 * DeviceSendResult result = DeviceRetryUtil.executeOnCode(
 *         () -> deviceGateway.sendCommand(...), // 每次尝试重新执行的动作
 *         1217,                                  // 响应中需要重试的业务码
 *         5,                                     // 最大尝试次数
 *         3000L);                                // 重试间隔(ms)
 * </pre>
 */
@Slf4j
public final class DeviceRetryUtil {

    private DeviceRetryUtil() {
    }

    /**
     * 执行发送动作并依据响应业务码重试。
     *
     * @param action      每次尝试执行的发送动作，返回带响应体的结果
     * @param retryCode   响应体 JSON 中 code 等于该值时触发重试
     * @param maxAttempts 最大尝试次数（&gt;=1）
     * @param intervalMs  两次尝试之间等待的毫秒数
     * @return 未触发重试（成功）的响应
     * @throws IllegalArgumentException maxAttempts 小于 1
     * @throws DeviceException          达到最大尝试次数仍返回 retryCode，或等待被中断
     */
    public static DeviceSendResult executeOnCode(Supplier<DeviceSendResult> action,
                                                 int retryCode,
                                                 int maxAttempts,
                                                 long intervalMs) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts 必须 >= 1，实际: " + maxAttempts);
        }

        DeviceSendResult result = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            result = action.get();
            if (!containsRetryCode(result, retryCode)) {
                return result;
            }
            log.warn("[DeviceRetryUtil] 响应业务码 {} 触发重试，第 {}/{} 次尝试结束 deviceTaskId={} response={}",
                    retryCode, attempt, maxAttempts,
                    result.getTaskId(),
                    result.getResponseBody());
            if (attempt < maxAttempts) {
                sleep(intervalMs);
            }
        }

        throw new DeviceException("DEVICE_CODE_RETRY_FAILED",
                "响应业务码 " + retryCode + "，尝试 " + maxAttempts + " 次后仍失败"
                        + ", response=" + (result != null ? result.getResponseBody() : ""));
    }

    /** 判断响应体 JSON 中的 code 是否等于目标业务码；非 JSON 或解析失败按不匹配处理 */
    private static boolean containsRetryCode(DeviceSendResult result, int retryCode) {
        if (result == null || result.getResponseBody() == null || result.getResponseBody().isBlank()) {
            return false;
        }
        try {
            JSONObject json = JSON.parseObject(result.getResponseBody());
            return json != null && json.getIntValue("code") == retryCode;
        } catch (Exception e) {
            log.debug("[DeviceRetryUtil] 响应体非合法 JSON，跳过业务码判断: {}", result.getResponseBody());
            return false;
        }
    }

    private static void sleep(long intervalMs) {
        if (intervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeviceException("设备重试等待被中断", e);
        }
    }
}
