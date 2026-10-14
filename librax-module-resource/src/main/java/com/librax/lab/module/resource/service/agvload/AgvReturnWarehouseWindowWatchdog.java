package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.framework.quartz.core.handler.JobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 下料仓储入库窗口兜底轮询 Job，跟补料侧 {@link AgvWarehouseWindowWatchdog} 同一个模式，
 * 补上此前一直缺失的对称实现。
 *
 * <p>{@code AgvReturnPlanServiceImpl#requestNextWarehouseReturn} 请求仓储入库失败（比如
 * 一个波次卸料后，第一件的入库请求就被仓储拒绝）时，会把这次失败的副作用
 * （{@code warehouseRequestId}）撤销干净、失败不重试，正常靠同一波次内其它物料入库完成
 * 的回调事件链自然重新触发。如果失败的恰好是这一波唯一/最后一个待请求的物料，
 * 之后不会再有任何回调触发重试，会永久卡在 WAIT_WAREHOUSE，靠这个Job定期兜底。
 *
 * <p>安全性：巡检对每个未终态下料计划都会尝试重新请求最早一条待处理物料（不限 currentWave），
 * 但底层只会选中"状态IN_TRANSIT且warehouseRequestId为空"的物料——正常运行中的计划要么没有
 * 这种物料（还在AGV上、已完成，或请求正在途中），巡检什么也不发生；仓储仍有在途请求时也不会
 * 抢跑。只有真的卡住的物料才会被重新请求一次。
 *
 * <p>接入系统现成的 {@code infra_job} + {@link JobHandler} 定时任务框架，机制同
 * {@link AgvWarehouseWindowWatchdog} / {@link AgvOrchestrateQueueWatchdog}。
 */
@Slf4j
@Component("agvReturnWarehouseWindowWatchdogJob")
@RequiredArgsConstructor
public class AgvReturnWarehouseWindowWatchdog implements JobHandler {

    private final AgvReturnPlanService returnPlanService;

    @Override
    public String execute(String param) {
        try {
            returnPlanService.pokeActiveWarehousePlans();
        } catch (Exception e) {
            log.error("[AgvReturnWarehouseWindowWatchdog] 巡检异常", e);
            return "巡检异常: " + e.getMessage();
        }
        String result = "下料仓储窗口兜底巡检完成";
        log.info("[AgvReturnWarehouseWindowWatchdog] {}", result);
        return result;
    }
}
