package com.librax.lab.module.device.driver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.exception.DeviceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * HTTP 协议设备驱动
 *
 * <p>通过 REST 接口与设备通信。支持 GET/POST/PUT 方法，
 * 支持 NONE/BASIC/TOKEN/HMAC 四种认证方式。
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

    private final RestTemplate restTemplate;

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
        // 1. 构建 URL
        String url = buildUrl(device, command, executionId, callbackToken);

        // 2. 构建请求头
        HttpHeaders headers = buildHeaders(device, command, callbackToken, requestBody);

        // 3. 构建请求实体
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 4. 确定 HTTP 方法
        HttpMethod method = resolveMethod(command.getHttpMethod());

        log.info("[HttpDeviceDriver] 发送请求 url={} method={} deviceId={}",
                url, method, device.getDeviceId());

        try {
            // 5. 执行请求
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

            // 6. 提取 taskId
            String taskId = extractTaskId(response.getBody());
            log.info("[HttpDeviceDriver] 请求成功 deviceId={} taskId={} status={}",
                    device.getDeviceId(), taskId, response.getStatusCode());
            return taskId;

        } catch (Exception e) {
            throw new DeviceException("HTTP_SEND_FAILED",
                    "HTTP 指令发送失败: " + device.getDeviceId() + " - " + e.getMessage(), e);
        }
    }

    @Override
    public void cancel(DeviceInfoDO device, String taskId) {
        // v1 版本暂不支持 HTTP 设备取消，后续可配置取消端点
        log.warn("[HttpDeviceDriver] HTTP 设备暂不支持取消 deviceId={} taskId={}",
                device.getDeviceId(), taskId);
    }

    // ----------------------------------------------------------------
    // URL 构建
    // ----------------------------------------------------------------

    private String buildUrl(DeviceInfoDO device, DeviceCommandDO command,
                            String executionId, String callbackToken) {
        String baseUrl = device.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            // fallback: 用 host + port 构建
            baseUrl = "http://" + device.getHost() + ":" + device.getPort();
        }
        // 去掉尾部斜杠
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String path = command.getHttpPath() != null ? command.getHttpPath() : "";
        // 替换路径中的占位符
        path = path.replace("${executionId}", executionId != null ? executionId : "")
                .replace("${callbackToken}", callbackToken != null ? callbackToken : "");

        return baseUrl + path;
    }

    // ----------------------------------------------------------------
    // 请求头构建
    // ----------------------------------------------------------------

    private HttpHeaders buildHeaders(DeviceInfoDO device, DeviceCommandDO command,
                                     String callbackToken, String requestBody) {
        HttpHeaders headers = new HttpHeaders();

        // Content-Type
        String contentType = command.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        // 回调令牌
        if (callbackToken != null) {
            headers.set("X-Callback-Token", callbackToken);
        }

        // 认证
        applyAuth(headers, device, requestBody);

        return headers;
    }

    private void applyAuth(HttpHeaders headers, DeviceInfoDO device, String requestBody) {
        String authType = device.getAuthType();
        if (authType == null || "NONE".equals(authType)) {
            return;
        }

        String authConfig = device.getAuthConfig();
        if (authConfig == null || authConfig.isEmpty()) {
            return;
        }

        JSONObject config = JSON.parseObject(authConfig);

        switch (authType) {
            case "BASIC": {
                String username = config.getString("username");
                String password = config.getString("password");
                String credentials = Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
                break;
            }
            case "TOKEN": {
                String token = config.getString("token");
                String headerName = config.getString("headerName");
                if (headerName == null || headerName.isEmpty()) {
                    headerName = HttpHeaders.AUTHORIZATION;
                }
                String prefix = config.getString("prefix");
                if (prefix == null) {
                    prefix = "Bearer ";
                }
                headers.set(headerName, prefix + token);
                break;
            }
            case "HMAC": {
                String secret = config.getString("secret");
                String algorithm = config.getString("algorithm");
                if (algorithm == null) {
                    algorithm = "HmacSHA256";
                }
                try {
                    javax.crypto.Mac mac = javax.crypto.Mac.getInstance(algorithm);
                    mac.init(new javax.crypto.spec.SecretKeySpec(
                            secret.getBytes(StandardCharsets.UTF_8), algorithm));
                    byte[] sig = mac.doFinal(
                            (requestBody != null ? requestBody : "").getBytes(StandardCharsets.UTF_8));
                    headers.set("X-Signature", Base64.getEncoder().encodeToString(sig));
                    headers.set("X-Signature-Algorithm", algorithm);
                } catch (Exception e) {
                    throw new DeviceException("AUTH_FAILED", "HMAC 签名失败: " + e.getMessage(), e);
                }
                break;
            }
            default:
                log.warn("[HttpDeviceDriver] 未知认证类型: {}", authType);
        }
    }

    // ----------------------------------------------------------------
    // 工具方法
    // ----------------------------------------------------------------

    private HttpMethod resolveMethod(String method) {
        if (method == null || method.isEmpty()) {
            return HttpMethod.POST;
        }
        switch (method.toUpperCase()) {
            case "GET":
                return HttpMethod.GET;
            case "PUT":
                return HttpMethod.PUT;
            case "DELETE":
                return HttpMethod.DELETE;
            default:
                return HttpMethod.POST;
        }
    }

    /**
     * 尝试从响应体中提取 taskId，失败则生成 UUID
     */
    private String extractTaskId(String responseBody) {
        if (responseBody != null && !responseBody.isEmpty()) {
            try {
                JSONObject json = JSON.parseObject(responseBody);
                // 尝试常见字段名
                String taskId = json.getString("taskId");
                if (taskId == null) taskId = json.getString("task_id");
                if (taskId == null) taskId = json.getString("id");
                if (taskId != null && !taskId.isEmpty()) {
                    return taskId;
                }
            } catch (Exception ignored) {
                // 响应不是 JSON，忽略
            }
        }
        return UUID.randomUUID().toString();
    }
}
