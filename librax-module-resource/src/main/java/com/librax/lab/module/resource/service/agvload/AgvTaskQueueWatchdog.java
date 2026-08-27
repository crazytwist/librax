package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.framework.quartz.core.handler.JobHandler;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvTaskQueueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AGV 设备指令队列（{@code lab_agv_task_queue}）兜底轮询 Job。
 *
 * <p>正常情况下 {@code AgvQueueService} 里"AGV拒绝接收指令"会触发一次2秒后的自动重试
 * （{@code retryDispatchAfterRejectionAsync}），事件驱动即可自愈。这个 Job 是给两种
 * 例外情况兜底的：重试本身又被拒绝（AGV异常繁忙持续超过2秒）、或者重试还没跑完应用就重启了
 * （异步任务在内存里排队，重启会丢失）。两种情况下队列项会一直卡在 PENDING、设备没有
 * DISPATCHED 记录，表面看"空闲"实际上永远等不到下一次真正的调度尝试。
 *
 * <p>跟 {@link AgvOrchestrateQueueWatchdog} 是同一套 {@code infra_job} 兜底思路：正常巡检
 * 应该总是空转，只有真出现"看起来空闲但还有排队项"时才会触发一次补发。
 */
@Slf4j
@Component("agvTaskQueueWatchdogJob")
@RequiredArgsConstructor
public class AgvTaskQueueWatchdog implements JobHandler {

    private final AgvTaskQueueMapper queueMapper;
    private final AgvQueueService queueService;

    @Override
    public String execute(String param) {
        int checked = 0;
        for (String deviceId : queueMapper.selectDistinctDeviceIds()) {
            checked++;
            try {
                queueService.dispatchNextIfIdle(deviceId);
            } catch (Exception e) {
                log.error("[AgvTaskQueueWatchdog] 巡检触发失败 deviceId={}", deviceId, e);
            }
        }
        String result = String.format("巡检%d台设备的指令队列", checked);
        log.info("[AgvTaskQueueWatchdog] {}", result);
        return result;
    }
}
