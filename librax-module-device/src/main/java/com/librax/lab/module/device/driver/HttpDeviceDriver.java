package com.librax.lab.module.device.driver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.librax.lab.framework.common.util.expression.ExpressionUtil;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.exception.DeviceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * HTTP 协议设备驱动
 *
 * <p>通过 REST 接口与设备通信。认证（BASIC / TOKEN / HMAC）由
 * {@link DeviceHttpClientFactory} 内置的拦截器统一处理，
 * 本类只负责 URL 构建、请求发送和响应解析。
 *
 * <p>回调模式：
 * <ul>
 *   <li>WEBHOOK：设备完成后主动 POST 到回调 URL</li>
 *   <li>POLL：由 {@code DevicePollScheduler} 定期轮询设备获取结果</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpDeviceDriver implements DeviceDriver {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final DeviceHttpClientFactory clientFactory;

    @Override
    public String supportProtocol() {
        return "HTTP";
    }

    @Override
    public String send(DeviceInfoDO device,
                       DeviceCommandDO command,
                       String requestBody,
                       String executionId,
                       String callbackToken) {
        String url = buildUrl(device, command, executionId, callbackToken);
        String method = resolveMethod(command.getHttpMethod());

        Request request = buildRequest(url, method, command, requestBody, callbackToken);

        log.info("[HttpDeviceDriver] 发送请求 url={} method={} deviceId={}", url, method, device.getDeviceId());

        OkHttpClient client = clientFactory.getClient(device);
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new DeviceException("HTTP_SEND_FAILED",
                        "HTTP 请求失败 status=" + response.code() + " deviceId=" + device.getDeviceId());
            }
            String taskId = extractTaskId(body);
            log.info("[HttpDeviceDriver] 请求成功 deviceId={} taskId={} status={}",
                    device.getDeviceId(), taskId, response.code());
            return taskId;
        } catch (DeviceException e) {
            throw e;
        } catch (Exception e) {
            throw new DeviceException("HTTP_SEND_FAILED",
                    "HTTP 指令发送失败: " + device.getDeviceId() + " - " + e.getMessage(), e);
        }
    }

    @Override
    public void cancel(DeviceInfoDO device, String taskId) {
        log.warn("[HttpDeviceDriver] HTTP 设备暂不支持取消 deviceId={} taskId={}",
                device.getDeviceId(), taskId);
    }

    // ----------------------------------------------------------------
    // 请求构建
    // ----------------------------------------------------------------

    private Request buildRequest(String url, String method,
                                 DeviceCommandDO command, String requestBody, String callbackToken) {
        Request.Builder builder = new Request.Builder().url(url);

        // 回调令牌（业务头，与认证头分开）
        if (callbackToken != null && !callbackToken.isBlank()) {
            builder.header("X-Callback-Token", callbackToken);
        }

        // Content-Type
        MediaType mediaType = resolveMediaType(command.getContentType());

        // Body（GET / DELETE 无 body）
        if ("GET".equals(method) || "DELETE".equals(method)) {
            builder.method(method, null);
        } else {
            RequestBody body = RequestBody.create(
                    requestBody != null ? requestBody : "", mediaType);
            builder.method(method, body);
        }

        return builder.build();
    }

    // ----------------------------------------------------------------
    // URL 构建
    // ----------------------------------------------------------------

    private String buildUrl(DeviceInfoDO device, DeviceCommandDO command,
                            String executionId, String callbackToken) {
        String baseUrl = device.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://" + device.getHost() + ":" + device.getPort();
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String path = command.getHttpPath() != null ? command.getHttpPath() : "";
        path = ExpressionUtil.render(path,
                Map.of("executionId", executionId != null ? executionId : "",
                        "callbackToken", callbackToken != null ? callbackToken : ""));

        return baseUrl + path;
    }

    // ----------------------------------------------------------------
    // 工具方法
    // ----------------------------------------------------------------

    private String resolveMethod(String method) {
        if (method == null || method.isBlank()) return "POST";
        return switch (method.toUpperCase()) {
            case "GET" -> "GET";
            case "PUT" -> "PUT";
            case "DELETE" -> "DELETE";
            default -> "POST";
        };
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            MediaType mt = MediaType.parse(contentType);
            if (mt != null) return mt;
        }
        return JSON_MEDIA_TYPE;
    }

    /**
     * 尝试从响应体中提取 taskId，失败则生成 UUID。
     */
    private String extractTaskId(String responseBody) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JSONObject json = JSON.parseObject(responseBody);
                String taskId = json.getString("taskId");
                if (taskId == null) taskId = json.getString("task_id");
                if (taskId == null) taskId = json.getString("id");
                if (taskId != null && !taskId.isBlank()) {
                    return taskId;
                }
            } catch (Exception ignored) {
                // 响应不是 JSON，忽略
            }
        }
        return UUID.randomUUID().toString();
    }
}
