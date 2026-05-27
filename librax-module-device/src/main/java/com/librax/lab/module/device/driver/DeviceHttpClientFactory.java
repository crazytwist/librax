package com.librax.lab.module.device.driver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.exception.DeviceException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 设备客户端工厂
 *
 * <p>按 deviceId 缓存 {@link OkHttpClient}，每个客户端预置了该设备的：
 * <ul>
 *   <li>连接 / 读取超时（来自 {@link DeviceInfoDO}）</li>
 *   <li>认证拦截器（NONE / BASIC / TOKEN / HMAC）</li>
 *   <li>TOKEN 类型额外挂载 401 自动刷新 Authenticator</li>
 * </ul>
 *
 * <p>当设备配置变更时，调用 {@link #invalidate(String)} 淘汰旧实例，
 * 下次 {@link #getClient} 调用时自动重建。
 */
@Slf4j
@Component
public class DeviceHttpClientFactory {

    /** deviceId -> OkHttpClient，线程安全 */
    private final ConcurrentHashMap<String, OkHttpClient> clientCache = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // 公共 API
    // ----------------------------------------------------------------

    /**
     * 获取设备专属 HTTP 客户端（缓存命中直接返回）
     */
    public OkHttpClient getClient(DeviceInfoDO device) {
        return clientCache.computeIfAbsent(device.getDeviceId(), id -> buildClient(device));
    }

    /**
     * 设备配置变更后调用，淘汰旧客户端；下次请求时按最新配置重建
     */
    public void invalidate(String deviceId) {
        OkHttpClient old = clientCache.remove(deviceId);
        if (old != null) {
            old.dispatcher().executorService().shutdown();
            old.connectionPool().evictAll();
            log.info("[DeviceHttpClientFactory] 淘汰旧客户端 deviceId={}", deviceId);
        }
    }

    // ----------------------------------------------------------------
    // 构建
    // ----------------------------------------------------------------

    private OkHttpClient buildClient(DeviceInfoDO device) {
        long connectMs = device.getConnectTimeoutMs() != null ? device.getConnectTimeoutMs() : 5_000L;
        long readMs    = device.getReadTimeoutMs()    != null ? device.getReadTimeoutMs()    : 30_000L;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectMs, TimeUnit.MILLISECONDS)
                .readTimeout(readMs, TimeUnit.MILLISECONDS)
                .writeTimeout(readMs, TimeUnit.MILLISECONDS)
                .addInterceptor(new DeviceAuthInterceptor(device));

        // TOKEN 类型额外配置 401 自动刷新
        if ("TOKEN".equals(device.getAuthType())) {
            builder.authenticator(new DeviceTokenAuthenticator(device));
        }

        log.info("[DeviceHttpClientFactory] 构建客户端 deviceId={} authType={} connectTimeout={}ms readTimeout={}ms",
                device.getDeviceId(), device.getAuthType(), connectMs, readMs);
        return builder.build();
    }

    // ================================================================
    // 内部类：认证拦截器
    // ================================================================

    /**
     * 每个出站请求前注入认证头，支持 BASIC / TOKEN / HMAC。
     */
    static class DeviceAuthInterceptor implements Interceptor {

        private final DeviceInfoDO device;

        DeviceAuthInterceptor(DeviceInfoDO device) {
            this.device = device;
        }

        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();
            applyAuth(builder, original);
            return chain.proceed(builder.build());
        }

        private void applyAuth(Request.Builder builder, Request original) {
            String authType = device.getAuthType();
            if (authType == null || "NONE".equals(authType)) {
                return;
            }
            String authConfigStr = device.getAuthConfig();
            if (authConfigStr == null || authConfigStr.isBlank()) {
                return;
            }

            JSONObject config = JSON.parseObject(authConfigStr);

            switch (authType) {
                case "BASIC" -> applyBasic(builder, config);
                case "TOKEN" -> applyToken(builder, config);
                case "HMAC"  -> applyHmac(builder, original, config);
                default -> log.warn("[DeviceAuthInterceptor] 未知认证类型: {} deviceId={}",
                        authType, device.getDeviceId());
            }
        }

        private void applyBasic(Request.Builder builder, JSONObject config) {
            String username = config.getString("username");
            String password = config.getString("password");
            String credential = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + credential);
        }

        private void applyToken(Request.Builder builder, JSONObject config) {
            String token      = config.getString("token");
            String headerName = config.getString("headerName");
            String prefix     = config.getString("prefix");
            if (headerName == null || headerName.isBlank()) headerName = "Authorization";
            if (prefix == null) prefix = "Bearer ";
            builder.header(headerName, prefix + token);
        }

        /**
         * HMAC 签名：需要读取并重建 RequestBody，以便 OkHttp 仍能发送原始内容。
         */
        private void applyHmac(Request.Builder builder, Request original, JSONObject config) {
            String secret    = config.getString("secret");
            String algorithm = config.getString("algorithm");
            if (algorithm == null) algorithm = "HmacSHA256";

            try {
                // 读取 body 字节，同时重建 RequestBody
                byte[] bodyBytes  = readAndRebuildBody(builder, original);

                Mac mac = Mac.getInstance(algorithm);
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
                byte[] sig = mac.doFinal(bodyBytes);

                builder.header("X-Signature", Base64.getEncoder().encodeToString(sig));
                builder.header("X-Signature-Algorithm", algorithm);
            } catch (DeviceException e) {
                throw e;
            } catch (Exception e) {
                throw new DeviceException("AUTH_FAILED", "HMAC 签名失败: " + e.getMessage(), e);
            }
        }

        /**
         * 将 RequestBody 写入 Buffer 读取内容，并用相同内容重建 RequestBody。
         * 返回 body 的字节内容（无 body 时返回空数组）。
         */
        private byte[] readAndRebuildBody(Request.Builder builder, Request original) throws IOException {
            RequestBody originalBody = original.body();
            if (originalBody == null) {
                return new byte[0];
            }
            Buffer buffer = new Buffer();
            originalBody.writeTo(buffer);
            byte[] bodyBytes = buffer.readByteArray();
            // 用读取到的字节重建 RequestBody，保留原 ContentType
            RequestBody newBody = RequestBody.create(bodyBytes, originalBody.contentType());
            builder.method(original.method(), newBody);
            return bodyBytes;
        }
    }

    // ================================================================
    // 内部类：Token 自动刷新 Authenticator
    // ================================================================

    /**
     * 收到 401 时自动刷新 token 并重试。
     *
     * <p>authConfig 中需包含 {@code tokenUrl} 字段才会尝试刷新；
     * 若未配置或刷新失败则直接放弃（返回 null），避免无限重试。
     *
     * <p>扩展点：在 {@link #doRefreshToken} 中实现具体的刷新协议
     * （OAuth2 client_credentials、自定义登录接口等）。
     */
    static class DeviceTokenAuthenticator implements Authenticator {

        private final DeviceInfoDO device;

        DeviceTokenAuthenticator(DeviceInfoDO device) {
            this.device = device;
        }

        @Override
        public Request authenticate(Route route, @NotNull Response response) {
            // 防止无限重试：先前已经重试过一次就放弃
            if (priorResponseCount(response) >= 1) {
                log.warn("[DeviceTokenAuthenticator] token 刷新后仍 401，放弃重试 deviceId={}",
                        device.getDeviceId());
                return null;
            }

            String tokenUrl = parseTokenUrl();
            if (tokenUrl == null) {
                log.warn("[DeviceTokenAuthenticator] authConfig 未配置 tokenUrl，无法自动刷新 deviceId={}",
                        device.getDeviceId());
                return null;
            }

            String newToken = doRefreshToken(tokenUrl);
            if (newToken == null) {
                return null;
            }

            JSONObject config = JSON.parseObject(device.getAuthConfig());
            String headerName = config.getString("headerName");
            String prefix     = config.getString("prefix");
            if (headerName == null || headerName.isBlank()) headerName = "Authorization";
            if (prefix == null) prefix = "Bearer ";

            return response.request().newBuilder()
                    .header(headerName, prefix + newToken)
                    .build();
        }

        private String parseTokenUrl() {
            String authConfig = device.getAuthConfig();
            if (authConfig == null || authConfig.isBlank()) return null;
            return JSON.parseObject(authConfig).getString("tokenUrl");
        }

        /**
         * 扩展点：实现真实的 token 刷新逻辑。
         * 例如：POST tokenUrl with {@code grant_type=client_credentials}。
         *
         * @return 新 token，获取失败返回 null
         */
        private String doRefreshToken(String tokenUrl) {
            // TODO: 按设备实际授权协议实现，示例：
            // OkHttpClient plain = new OkHttpClient();
            // Request req = new Request.Builder().url(tokenUrl)
            //     .post(RequestBody.create("{...}", MediaType.parse("application/json")))
            //     .build();
            // try (Response resp = plain.newCall(req).execute()) {
            //     return JSON.parseObject(resp.body().string()).getString("access_token");
            // }
            log.info("[DeviceTokenAuthenticator] 尝试刷新 token tokenUrl={} deviceId={}", tokenUrl, device.getDeviceId());
            return null;
        }

        private int priorResponseCount(Response response) {
            int count = 0;
            Response prior = response.priorResponse();
            while (prior != null) {
                count++;
                prior = prior.priorResponse();
            }
            return count;
        }
    }
}
