package com.librax.lab.module.device.driver;

import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.exception.DeviceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * TCP 协议设备驱动
 *
 * <p>短连接模式：connect → send → close。
 * 设备完成后通过 webhook 回调通知系统，或由 {@code DevicePollScheduler} 轮询获取结果。
 *
 * <p>支持三种编码方式（由 {@code DeviceCommandDO.contentType} 指定）：
 * <ul>
 *   <li>HEX：十六进制字符串，如 "0102030A"</li>
 *   <li>ASCII：文本字符串，UTF-8 编码</li>
 *   <li>BINARY：Base64 编码的二进制数据</li>
 * </ul>
 */
@Slf4j
@Component
public class TcpDeviceDriver implements DeviceDriver {

    @Override
    public String supportProtocol() {
        return "TCP";
    }

    @Override
    public String send(DeviceInfoDO device,
                       DeviceCommandDO command,
                       String requestBody,
                       String executionId,
                       String callbackToken) {
        String host = device.getHost();
        int port = device.getPort();
        int connectTimeout = device.getConnectTimeoutMs() != null
                ? device.getConnectTimeoutMs().intValue() : 5000;
        int readTimeout = device.getReadTimeoutMs() != null
                ? device.getReadTimeoutMs().intValue() : 10000;

        String taskId = UUID.randomUUID().toString();

        log.info("[TcpDeviceDriver] 连接设备 {}:{} deviceId={} timeout={}",
                host, port, device.getDeviceId(), connectTimeout);

        try (Socket socket = new Socket()) {
            // 1. 建立连接
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            socket.setSoTimeout(readTimeout);

            // 2. 编码请求数据
            byte[] payload = encodePayload(requestBody, command.getContentType());

            // 3. 发送数据
            OutputStream out = socket.getOutputStream();
            out.write(payload);
            out.flush();

            log.info("[TcpDeviceDriver] 数据已发送 deviceId={} bytes={} taskId={}",
                    device.getDeviceId(), payload.length, taskId);

            // 4. 对于 WEBHOOK 模式：发送后直接关闭，等待设备回调
            //    对于需要同步读取响应的场景，在此读取
            if ("POLL".equals(device.getCallbackMode())) {
                // POLL 模式下也是发送后关闭，由 DevicePollScheduler 轮询获取结果
                log.debug("[TcpDeviceDriver] POLL 模式，发送完毕等待轮询 taskId={}", taskId);
            } else {
                // WEBHOOK 模式 或 默认：尝试读取同步响应（如果有的话）
                try {
                    InputStream in = socket.getInputStream();
                    byte[] buf = new byte[4096];
                    int len = in.read(buf);
                    if (len > 0) {
                        String response = decodeResponse(buf, len, command.getContentType());
                        log.info("[TcpDeviceDriver] 收到同步响应 deviceId={} len={}", device.getDeviceId(), len);
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // 无同步响应，正常（异步设备）
                    log.debug("[TcpDeviceDriver] 无同步响应（异步设备）deviceId={}", device.getDeviceId());
                }
            }

            return taskId;

        } catch (DeviceException e) {
            throw e;
        } catch (Exception e) {
            throw new DeviceException("TCP_CONNECT_FAILED",
                    "TCP 连接失败: " + host + ":" + port + " - " + e.getMessage(), e);
        }
    }

    @Override
    public void cancel(DeviceInfoDO device, String taskId) {
        // TCP 短连接模式下无法取消已发送的指令
        log.warn("[TcpDeviceDriver] TCP 设备暂不支持取消 deviceId={} taskId={}",
                device.getDeviceId(), taskId);
    }

    // ----------------------------------------------------------------
    // 编解码
    // ----------------------------------------------------------------

    private byte[] encodePayload(String requestBody, String contentType) {
        if (requestBody == null || requestBody.isEmpty()) {
            return new byte[0];
        }
        if (contentType == null) contentType = "ASCII";

        switch (contentType.toUpperCase()) {
            case "HEX":
                return hexToBytes(requestBody.replaceAll("[\\s\\-:]", ""));
            case "BINARY":
                return Base64.getDecoder().decode(requestBody);
            case "ASCII":
            default:
                return requestBody.getBytes(StandardCharsets.UTF_8);
        }
    }

    private String decodeResponse(byte[] buf, int len, String contentType) {
        if (contentType == null) contentType = "ASCII";
        switch (contentType.toUpperCase()) {
            case "HEX":
                return bytesToHex(buf, len);
            case "BINARY":
                return Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(buf, len));
            case "ASCII":
            default:
                return new String(buf, 0, len, StandardCharsets.UTF_8);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
}
