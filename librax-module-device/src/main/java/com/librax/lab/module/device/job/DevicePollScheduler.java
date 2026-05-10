package com.librax.lab.module.device.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.librax.lab.module.device.callback.DeviceCallbackHandler;
import com.librax.lab.module.device.controller.vo.DeviceCallbackReqVO;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 设备轮询调度器
 *
 * <p>用于 callbackMode=POLL 的设备。设备不主动推送结果，
 * 系统定期轮询设备端点获取任务状态，完成后触发回调处理。
 *
 * <p>使用 {@link ScheduledExecutorService} 而非 @Scheduled，
 * 因为每个设备/任务的轮询间隔不同，需要动态调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevicePollScheduler {

    private final RestTemplate restTemplate;
    private final DeviceCallbackHandler callbackHandler;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4,
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "device-poll-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            });

    /** 活跃的轮询任务，key = executionId:nodeId */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    /**
     * 启动轮询任务
     *
     * @param device        设备信息
     * @param command       指令配置
     * @param taskId        设备返回的任务 ID
     * @param executionId   执行实例 ID
     * @param nodeId        节点 ID
     * @param callbackToken 回调令牌
     */
    public void startPolling(DeviceInfoDO device,
                             DeviceCommandDO command,
                             String taskId,
                             String executionId,
                             String nodeId,
                             String callbackToken) {
        String pollKey = executionId + ":" + nodeId;

        // 构建轮询 URL
        String pollPath = command.getPollPath();
        if (pollPath == null || pollPath.isEmpty()) {
            log.warn("[DevicePollScheduler] 未配置 pollPath，跳过轮询 taskId={}", taskId);
            return;
        }
        String baseUrl = device.getBaseUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String pollUrl = baseUrl + pollPath.replace("${taskId}", taskId);

        long intervalMs = device.getPollIntervalMs() != null ? device.getPollIntervalMs() : 3000L;
        int maxTimes = command.getPollMaxTimes() != null ? command.getPollMaxTimes() : 60;
        String doneExpr = command.getPollDoneExpr();

        AtomicInteger pollCount = new AtomicInteger(0);

        log.info("[DevicePollScheduler] 启动轮询 pollKey={} url={} interval={}ms maxTimes={}",
                pollKey, pollUrl, intervalMs, maxTimes);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            int count = pollCount.incrementAndGet();
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(pollUrl, String.class);
                String body = response.getBody();

                log.debug("[DevicePollScheduler] 轮询 #{} pollKey={} status={}",
                        count, pollKey, response.getStatusCode());

                // 检查是否完成
                if (isDone(body, doneExpr)) {
                    log.info("[DevicePollScheduler] 轮询完成 pollKey={} count={}", pollKey, count);
                    cancelPoll(pollKey);

                    // 构建回调请求
                    DeviceCallbackReqVO req = new DeviceCallbackReqVO();
                    req.setCallbackToken(callbackToken);
                    req.setSuccess(true);
                    req.setDeviceType(command.getDeviceType());
                    req.setCommandCode(command.getCommandCode());
                    req.setRawResponse(body);
                    callbackHandler.handle(executionId, nodeId, req);
                }

                // 超过最大次数
                if (count >= maxTimes) {
                    log.warn("[DevicePollScheduler] 轮询超时 pollKey={} count={}/{}", pollKey, count, maxTimes);
                    cancelPoll(pollKey);

                    DeviceCallbackReqVO req = new DeviceCallbackReqVO();
                    req.setCallbackToken(callbackToken);
                    req.setSuccess(false);
                    req.setErrorCode("POLL_TIMEOUT");
                    req.setErrorMsg("轮询超时，已尝试 " + count + " 次");
                    callbackHandler.handle(executionId, nodeId, req);
                }

            } catch (Exception e) {
                log.warn("[DevicePollScheduler] 轮询异常 #{} pollKey={}: {}", count, pollKey, e.getMessage());
                // 轮询异常不立即失败，继续重试直到超时
                if (count >= maxTimes) {
                    cancelPoll(pollKey);
                    DeviceCallbackReqVO req = new DeviceCallbackReqVO();
                    req.setCallbackToken(callbackToken);
                    req.setSuccess(false);
                    req.setErrorCode("POLL_FAILED");
                    req.setErrorMsg("轮询失败: " + e.getMessage());
                    callbackHandler.handle(executionId, nodeId, req);
                }
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        activeTasks.put(pollKey, future);
    }

    /**
     * 取消轮询
     */
    public void cancelPoll(String pollKey) {
        ScheduledFuture<?> future = activeTasks.remove(pollKey);
        if (future != null) {
            future.cancel(false);
            log.debug("[DevicePollScheduler] 轮询已取消 pollKey={}", pollKey);
        }
    }

    /**
     * 检查轮询结果是否表示完成
     */
    private boolean isDone(String responseBody, String doneExpr) {
        if (doneExpr == null || doneExpr.isEmpty() || responseBody == null) {
            return false;
        }

        try {
            Object document = JSON.parse(responseBody);

            // 支持简单的 JSONPath == value 表达式，如 $.status == "DONE"
            if (doneExpr.contains("==")) {
                String[] parts = doneExpr.split("==", 2);
                String jsonPath = parts[0].trim();
                String expectedValue = parts[1].trim().replaceAll("[\"']", "");

                Object actual = JSONPath.eval(document, jsonPath);
                return expectedValue.equals(String.valueOf(actual));
            }

            // 支持简单 boolean 类型的 JSONPath，如 $.completed
            Object result = JSONPath.eval(document, doneExpr);
            return Boolean.TRUE.equals(result) || "true".equalsIgnoreCase(String.valueOf(result));

        } catch (Exception e) {
            log.debug("[DevicePollScheduler] 完成表达式评估失败: {}", e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
        log.info("[DevicePollScheduler] 调度器已关闭，活跃任务数: {}", activeTasks.size());
    }
}
