package com.librax.lab.module.task.spi;

import com.librax.lab.module.flow.api.callback.TaskCallbackSpi;
import com.librax.lab.module.task.dispatch.TaskCallbackDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * {@link TaskCallbackSpi} 的任务模块实现
 *
 * <p>当设备通过 WEBHOOK 回调时，{@code DeviceCallbackHandler} 在推进 DAG 后
 * 通过此 SPI 关闭对应的 {@code lab_task} 记录，防止 Watchdog 误判超时并重新入队
 * 导致指令重复发送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCallbackSpiImpl implements TaskCallbackSpi {

    private final TaskCallbackDispatcher taskCallbackDispatcher;

    @Override
    public void closeByToken(String callbackToken,
                             boolean success,
                             Map<String, Object> outputs,
                             String errorCode,
                             String errorMsg) {
        if (callbackToken == null || callbackToken.isBlank()) {
            log.debug("[TaskCallbackSpi] callbackToken 为空，跳过任务关闭");
            return;
        }
        log.debug("[TaskCallbackSpi] 通过 token 关闭任务 token={} success={}", callbackToken, success);
        try {
            taskCallbackDispatcher.dispatch(callbackToken, success, outputs, errorCode, errorMsg);
        } catch (Exception e) {
            // 任务关闭失败不影响主流程（DAG 已推进），仅记录告警
            log.warn("[TaskCallbackSpi] 关闭任务异常 token={} error={}", callbackToken, e.getMessage(), e);
        }
    }
}
