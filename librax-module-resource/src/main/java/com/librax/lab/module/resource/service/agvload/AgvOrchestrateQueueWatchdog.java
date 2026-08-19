package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.framework.quartz.core.handler.JobHandler;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvOrchestrateQueueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AGV 编排排队（{@code lab_agv_orchestrate_queue}）兜底轮询 Job。
 *
 * <p>接入现成的 {@code infra_job} + {@link JobHandler} 定时任务框架，跟
 * {@link AgvIdleChargingWatchdog} 是同一套机制，不再自带 {@code @Scheduled}。
 *
 * <p>排队重试的主路径是事件驱动：{@code AgvLoadPlanServiceImpl}/{@code AgvReturnPlanServiceImpl}
 * 在任何会释放 AGV/目标/中转槽位的地方（{@code commitTransfer}、{@code markSlotReturned}、
 * {@code afterUnloadComplete}、AGV子任务失败释放槽位等）都会在事务提交后异步唤醒一次
 * {@code tryDispatchQueuedOrchestrationAsync}，正常情况下排队请求应该在槽位释放的瞬间
 * 就被立刻重试，不需要轮询。
 *
 * <p>但这套"唤醒点"是靠约定维护的：谁在新代码路径里新增了一处槽位释放，如果忘了同步调用
 * 唤醒，对应的排队记录就会一直卡在 PENDING，直到不相关的另一次唤醒恰好把它带上才会被看到——
 * 如果始终没有其它唤醒发生，就会永远卡住，没有编译期或运行时机制能保证这一点。这个 Job
 * 就是防止这种"唤醒点漏写"或"唤醒异步任务执行时机器重启丢失"场景的兜底：定期扫一遍所有
 * 还有 PENDING 记录的设备，对每个设备分别触发一次 LOAD 和 RETURN 两条编排服务各自的
 * {@code tryDispatchQueuedOrchestrationAsync}（两个方法内部各自只处理自己 planType 的
 * 队首记录，互不影响，调用另一边的没有副作用）。
 *
 * <p>正常情况下每次巡检应该都是空转（事件驱动早就处理完了），只有在漏唤醒时才会真正触发一次
 * 有意义的重试，所以巡检周期不需要很短，作为兜底即可。
 */
@Slf4j
@Component("agvOrchestrateQueueWatchdogJob")
@RequiredArgsConstructor
public class AgvOrchestrateQueueWatchdog implements JobHandler {

    private final AgvOrchestrateQueueMapper orchestrateQueueMapper;
    private final AgvLoadPlanService loadPlanService;
    private final AgvReturnPlanService returnPlanService;

    @Override
    public String execute(String param) {
        int checked = 0;
        for (String deviceId : orchestrateQueueMapper.selectDistinctPendingDeviceIds()) {
            checked++;
            try {
                loadPlanService.tryDispatchQueuedOrchestrationAsync(deviceId);
                returnPlanService.tryDispatchQueuedOrchestrationAsync(deviceId);
            } catch (Exception e) {
                log.error("[AgvOrchestrateQueueWatchdog] 巡检触发失败 deviceId={}", deviceId, e);
            }
        }
        String result = String.format("巡检%d台设备存在的排队编排请求", checked);
        log.info("[AgvOrchestrateQueueWatchdog] {}", result);
        return result;
    }
}
