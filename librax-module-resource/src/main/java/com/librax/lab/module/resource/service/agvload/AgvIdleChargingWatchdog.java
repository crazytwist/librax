package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.framework.quartz.core.handler.JobHandler;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvReturnPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvTaskQueueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AGV 空闲自动充电巡检 Job。
 *
 * <p>接入系统现成的定时任务框架（{@code infra_job} + {@link JobHandler}，见
 * {@code JobController}/{@code JobHandlerInvoker}），不用自己写 {@code @Scheduled}——
 * 调度频率、启停、手动触发、执行日志全部走管理后台"定时任务"那一套，跟别的 Job（比如
 * payNotifyJob）是同一个机制。执行入口 {@link #execute}，由 Quartz 按 {@code infra_job}
 * 表里配置的 cron 表达式调用，本类不需要也不应该再自带调度注解。
 *
 * <p>每次执行：AGV 设备队列（{@code lab_agv_task_queue}）里既没有 DISPATCHED
 * 也没有 PENDING 记录，视为空闲，下发一次充电指令（{@code AGV.Charging}，
 * 命令本身是纯静态模板，不带任何占位符，直接发送即可）。
 *
 * <p>充电指令下发后不关心后续状态：
 * <ul>
 *   <li>指令本身 completion_mode=SYNC，HTTP 200 即视为已下发完成，不等待任何回调；</li>
 *   <li>AGV 支持在充电过程中直接接收并执行新任务，不需要本系统先"停止充电"再派活，
 *       所以充电指令不经过 {@link AgvQueueService} 那套设备忙碌排队机制，
 *       直接发送，即使极小概率跟一次真实任务派发撞在同一瞬间被拒绝，跳过等下一次执行重试即可，
 *       没有正确性影响。</li>
 * </ul>
 *
 * <p>去抖：只在"从有任务变成空闲"这个状态转换的瞬间下发一次，避免持续空闲期间每次执行都重复刷指令。
 * 复用 {@link AgvChargingStateTracker}（内存态）记录每台 AGV 当前这一段空闲期是否已经
 * 发过充电指令，一旦重新检测到有 DISPATCHED/PENDING 任务或活跃计划就重置，下一次空闲会
 * 重新触发。这个标记同时也是 {@link AgvQueueService} 判断"要不要先停止充电再下发任务"的
 * 依据，两边共用同一份状态，不再各自维护一份。应用重启后该状态会丢失，至多导致重启后的
 * 第一个空闲周期多发一次充电指令，无副作用。
 */
@Slf4j
@Component("agvIdleChargingJob")
@RequiredArgsConstructor
public class AgvIdleChargingWatchdog implements JobHandler {

    /** lab_device_command 里 device_type=AGV, command_code=Charging 那条指令（id=910182）。 */
    private static final String CHARGING_COMMAND_CODE = "Charging";

    private final AgvTaskQueueMapper queueMapper;
    private final AgvLoadPlanMapper loadPlanMapper;
    private final AgvReturnPlanMapper returnPlanMapper;
    private final DeviceCommandSpi deviceCommandSpi;
    private final AgvChargingStateTracker chargingStateTracker;

    @Override
    public String execute(String param) {
        int checked = 0;
        int charged = 0;
        for (String deviceId : queueMapper.selectDistinctDeviceIds()) {
            checked++;
            try {
                if (checkOne(deviceId)) charged++;
            } catch (Exception e) {
                log.error("[AgvIdleCharging] 巡检异常 deviceId={}", deviceId, e);
            }
        }
        String result = String.format("巡检%d台AGV，下发充电指令%d次", checked, charged);
        log.info("[AgvIdleCharging] {}", result);
        return result;
    }

    /** @return 本次是否实际下发了充电指令 */
    private boolean checkOne(String deviceId) {
        // 队列（lab_agv_task_queue）不够用来单独判断"空闲"：taskState=2 一到，队列项会
        // 立刻同步标记DONE（见 AgvLoadPlanServiceImpl#handle），但下一阶段的真正指令
        // （LOAD->MOVE / MOVE->UNLOAD，或补料逐件模式下一件LOAD）是等事务提交后才异步
        // 下发、重新入队的——这两者之间有一个短暂窗口，队列看起来"空"但AGV其实还在同一个
        // 计划的执行过程中，不是真的空闲。所以还要确认这台设备没有任何未终态的补料/下料
        // 计划，两边都确认"真没事干"才判定空闲，避免任务执行中途被插入充电指令。
        boolean idle = !queueMapper.hasDispatched(deviceId) && !queueMapper.hasPending(deviceId)
                && !loadPlanMapper.hasActivePlan(deviceId) && !returnPlanMapper.hasActivePlan(deviceId);

        if (!idle) {
            // 注意：这里不清除充电标记。只有 AgvQueueService 真正确认一次 startTask 被AGV
            // 接受之后，才代表AGV确定脱离了充电状态，由它来清除标记。这里如果也顺手清一次，
            // 会有一个真实的竞态：任务刚创建、plan状态刚变成非终态（hasActivePlan=true）但
            // AgvQueueService的停止充电+startTask还没来得及真正被AGV接受这段短窗口内，
            // 巡检恰好扫到这里会误清标记，导致AgvQueueService下一次重试时以为AGV已经不在
            // 充电、不再重发停止充电指令——如果当时Cancel确实还没生效，就会一直重试
            // startTask而不再尝试停止充电。标记只增不减地留给分发链路清除，成本只是"AGV
            // 已经自然结束充电、迟迟没有新任务"这段时间里状态显示得不够精确，下一次真正
            // 派发任务前会照常先发一次停止充电（哪怕这时候其实已经不在充电了也无所谓，
            // 该指令预期是幂等/无副作用的）。
            return false;
        }

        if (chargingStateTracker.isCharging(deviceId)) {
            return false; // 这段空闲期已经发过一次，不重复发
        }

        deviceCommandSpi.send("AGV", CHARGING_COMMAND_CODE,
                Map.of("disableSysFieldInjection", true),
                "SYSTEM-AGV-CHARGE-" + deviceId, "agv_idle_charging", null);
        chargingStateTracker.markCharging(deviceId);
        log.info("[AgvIdleCharging] AGV空闲，已下发充电指令 deviceId={}", deviceId);
        return true;
    }
}
