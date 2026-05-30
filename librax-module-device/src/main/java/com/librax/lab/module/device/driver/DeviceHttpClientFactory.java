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

        // TOKEN 类型：从 authConfig 读取初始 token，拦截器与刷新器共享 TokenHolder
        TokenHolder tokenHolder = null;
        if ("TOKEN".equals(device.getAuthType()) && device.getAuthConfig() != null) {
            JSONObject config = JSON.parseObject(device.getAuthConfig());
            String initialToken = config.getString("token");
            tokenHolder = new TokenHolder(initialToken);
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectMs, TimeUnit.MILLISECONDS)
                .readTimeout(readMs, TimeUnit.MILLISECONDS)
                .writeTimeout(readMs, TimeUnit.MILLISECONDS)
                .addInterceptor(new DeviceAuthInterceptor(device, tokenHolder));

        // TOKEN 类型额外配置 401 自动刷新
        if ("TOKEN".equals(device.getAuthType())) {
            builder.authenticator(new DeviceTokenAuthenticator(device, tokenHolder));
        }

        log.info("[DeviceHttpClientFactory] 构建客户端 deviceId={} authType={} connectTimeout={}ms readTimeout={}ms",
                device.getDeviceId(), device.getAuthType(), connectMs, readMs);
        return builder.build();
    }

    // ================================================================
    // 内部类：Token 持有者（拦截器与刷新器共享）
    // ================================================================

    /**
     * 线程安全的 token 持有者，供同一设备的拦截器和刷新器共享。
     */
    static class TokenHolder {
        /** 当前有效 token，volatile 保证可见性 */
        volatile String token;

        TokenHolder(String initial) {
            this.token = initial != null ? initial : "";
        }

        boolean isEmpty() {
            return token == null || token.isBlank();
        }
    }

    // ================================================================
    // 内部类：认证拦截器
    // ================================================================

    /**
     * 每个出站请求前注入认证头，支持 BASIC / TOKEN / HMAC。
     * TOKEN 类型与 {@link DeviceTokenAuthenticator} 共享同一个 {@link TokenHolder}，
     * 首次发请求若 token 为空则自动调用登录接口获取。
     */
    static class DeviceAuthInterceptor implements Interceptor {

        private final DeviceInfoDO device;
        /** TOKEN 模式下持有当前 token；BASIC/HMAC 时为 null */
        private final TokenHolder tokenHolder;

        DeviceAuthInterceptor(DeviceInfoDO device, TokenHolder tokenHolder) {
            this.device      = device;
            this.tokenHolder = tokenHolder;
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

        /**
         * TOKEN 模式：优先使用 TokenHolder 中的内存 token，
         * 若为空则同步调用登录接口获取（懒加载）。
         */
        private void applyToken(Request.Builder builder, JSONObject config) {
            // 懒加载：首次发请求时若 token 为空则主动登录
            if (tokenHolder != null && tokenHolder.isEmpty()) {
                String tokenUrl   = config.getString("tokenUrl");
                Object loginBody  = config.get("loginBody");
                String tokenField = config.getString("tokenField");
                if (tokenUrl != null && !tokenUrl.isBlank()) {
                    String fetched = fetchToken(tokenUrl, loginBody, tokenField);
                    if (fetched != null) {
                        tokenHolder.token = fetched;
                        log.info("[DeviceAuthInterceptor] 首次登录成功，获取 token deviceId={}", device.getDeviceId());
                    } else {
                        log.warn("[DeviceAuthInterceptor] 首次登录失败 deviceId={}", device.getDeviceId());
                    }
                }
            }

            String token      = tokenHolder != null && !tokenHolder.isEmpty()
                                ? tokenHolder.token
                                : config.getString("token");
            String headerName = config.getString("headerName");
            String prefix     = config.getString("prefix");
            if (headerName == null || headerName.isBlank()) headerName = "Authorization";
            if (prefix == null) prefix = "Bearer ";
            if (token != null && !token.isBlank()) {
                builder.header(headerName, prefix + token);
            }
        }

        /**
         * 调用外部登录接口获取 token，支持 JSON body POST。
         *
         * @param tokenUrl  登录 URL，例如 http://api.example.com/auth/login
         * @param loginBody 请求体 JSON 字符串，例如 {"username":"x","password":"y"}
         * @param tokenField 响应 JSON 中 token 的字段名，默认 access_token
         * @return token 字符串，失败返回 null
         */
        static String fetchToken(String tokenUrl, Object loginBodyRaw, String tokenField) {
            if (tokenField == null || tokenField.isBlank()) tokenField = "access_token";
            OkHttpClient plain = new OkHttpClient();
            // loginBody 支持 JSON 对象或字符串两种格式
            String body;
            if (loginBodyRaw == null) {
                body = "{}";
            } else if (loginBodyRaw instanceof String s) {
                body = s.isBlank() ? "{}" : s;
            } else {
                body = JSON.toJSONString(loginBodyRaw);
            }
            RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(tokenUrl).post(requestBody).build();
            try (Response response = plain.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("[DeviceAuth] 登录请求失败 status={} url={}", response.code(), tokenUrl);
                    return null;
                }
                String respStr = response.body().string();
                JSONObject respJson = JSON.parseObject(respStr);
                // 支持点号路径，如 data.accessToken
                String token = getByPath(respJson, tokenField);
                if (token == null || token.isBlank()) {
                    log.warn("[DeviceAuth] 响应中未找到 token 字段={} url={} body={}", tokenField, tokenUrl, respStr);
                }
                return token;
            } catch (Exception e) {
                log.warn("[DeviceAuth] 登录请求异常 url={} error={}", tokenUrl, e.getMessage());
                return null;
            }
        }

        /**
         * 按点号路径从 JSON 中取值，如 "data.accessToken"。
         */
        private static String getByPath(JSONObject json, String path) {
            String[] keys = path.split("\\.");
            Object current = json;
            for (String key : keys) {
                if (!(current instanceof JSONObject obj)) return null;
                current = obj.get(key);
            }
            return current != null ? current.toString() : null;
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
     * 收到 401 时自动重新登录刷新 token 并重试请求。
     *
     * <p>authConfig 中需包含 {@code tokenUrl} + {@code loginBody} 才能自动刷新；
     * 若未配置或刷新失败则直接放弃（返回 null），避免无限重试。
     */
    static class DeviceTokenAuthenticator implements Authenticator {

        private final DeviceInfoDO device;
        private final TokenHolder  tokenHolder;

        DeviceTokenAuthenticator(DeviceInfoDO device, TokenHolder tokenHolder) {
            this.device      = device;
            this.tokenHolder = tokenHolder;
        }

        @Override
        public Request authenticate(Route route, @NotNull Response response) {
            // 防止无限重试：先前已经重试过一次就放弃
            if (priorResponseCount(response) >= 1) {
                log.warn("[DeviceTokenAuthenticator] token 刷新后仍 401，放弃重试 deviceId={}",
                        device.getDeviceId());
                return null;
            }

            String authConfig = device.getAuthConfig();
            if (authConfig == null || authConfig.isBlank()) return null;
            JSONObject config = JSON.parseObject(authConfig);

            String tokenUrl   = config.getString("tokenUrl");
            Object loginBody  = config.get("loginBody");
            String tokenField = config.getString("tokenField");
            if (tokenUrl == null || tokenUrl.isBlank()) {
                log.warn("[DeviceTokenAuthenticator] authConfig 未配置 tokenUrl，无法自动刷新 deviceId={}",
                        device.getDeviceId());
                return null;
            }

            // 重新登录获取新 token
            log.info("[DeviceTokenAuthenticator] 收到 401，重新登录获取 token tokenUrl={} deviceId={}",
                    tokenUrl, device.getDeviceId());
            String newToken = DeviceAuthInterceptor.fetchToken(tokenUrl, loginBody, tokenField);
            if (newToken == null) {
                log.warn("[DeviceTokenAuthenticator] 重新登录失败 deviceId={}", device.getDeviceId());
                return null;
            }

            // 更新共享 token，下次请求拦截器直接使用新 token
            if (tokenHolder != null) {
                tokenHolder.token = newToken;
            }

            String headerName = config.getString("headerName");
            String prefix     = config.getString("prefix");
            if (headerName == null || headerName.isBlank()) headerName = "Authorization";
            if (prefix == null) prefix = "Bearer ";

            log.info("[DeviceTokenAuthenticator] token 刷新成功，重试请求 deviceId={}", device.getDeviceId());
            return response.request().newBuilder()
                    .header(headerName, prefix + newToken)
                    .build();
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
