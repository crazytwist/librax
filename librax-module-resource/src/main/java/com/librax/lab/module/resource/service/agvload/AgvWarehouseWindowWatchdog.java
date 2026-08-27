package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.framework.quartz.core.handler.JobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 补料仓储备料窗口兜底轮询 Job。
 *
 * <p>WAREHOUSE 逐件模式下，"向仓储请求下一件备料"这个动作现在改成了失败不重试
 * （见 {@code AgvLoadPlanServiceImpl#doRequestWarehouseItem} 的说明：不再在同步
 * 调用里持锁sleep重试），正常靠同一计划内其它物料就绪/被AGV抓走的事件链自然
 * 重新触发。极端情况下（比如恰好是最后一件的请求失败，之后再没有别的事件会
 * 触发它了）可能永久卡住，这个Job定期巡检所有未完成的补料计划兜底重试一次。
 *
 * <p>接入系统现成的 {@code infra_job} + {@link JobHandler} 定时任务框架，机制同
 * {@link AgvOrchestrateQueueWatchdog} / {@link AgvIdleChargingWatchdog}。
 */
@Slf4j
@Component("agvWarehouseWindowWatchdogJob")
@RequiredArgsConstructor
public class AgvWarehouseWindowWatchdog implements JobHandler {

    private final AgvLoadPlanService loadPlanService;

    @Override
    public String execute(String param) {
        try {
            loadPlanService.pokeActiveWarehousePlans();
        } catch (Exception e) {
            log.error("[AgvWarehouseWindowWatchdog] 巡检异常", e);
            return "巡检异常: " + e.getMessage();
        }
        String result = "补料仓储窗口兜底巡检完成";
        log.info("[AgvWarehouseWindowWatchdog] {}", result);
        return result;
    }
}
