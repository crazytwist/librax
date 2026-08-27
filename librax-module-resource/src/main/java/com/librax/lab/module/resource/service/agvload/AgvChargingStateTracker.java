package com.librax.lab.module.resource.service.agvload;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跟踪每台 AGV 当前是否处于"由本系统主动下发充电指令"的状态。
 *
 * <p>{@link AgvIdleChargingWatchdog} 是唯一会主动下发充电指令的地方，AGV 处于充电期间
 * 对新指令的响应能力可能受影响（跟"AGV运行中，无法响应"是同一类"暂时接不了新指令"的
 * 表现）——如果这时候补料/下料/转运需要用这台AGV，得先停止充电，否则 startTask 大概率
 * 被拒绝。{@link AgvQueueService} 在真正下发 startTask 前查询 {@link #isCharging}，
 * 如果是 true 就先发一次停止充电指令，成功下发 startTask 后再调用
 * {@link #markNotCharging} 清除标记。
 *
 * <p>纯内存态，不落库、不进Redis：应用重启后这个标记会丢失，最坏情况是重启后
 * AgvQueueService 不知道AGV正在充电，直接尝试下发任务被拒绝一次，走已有的"AGV拒绝接收
 * 指令，2秒后重试"机制即可自愈，不影响正确性，没必要为这么小的边界情况加持久化。
 */
@Component
public class AgvChargingStateTracker {

    private final Map<String, Boolean> charging = new ConcurrentHashMap<>();

    public boolean isCharging(String deviceId) {
        return Boolean.TRUE.equals(charging.get(deviceId));
    }

    public void markCharging(String deviceId) {
        charging.put(deviceId, true);
    }

    public void markNotCharging(String deviceId) {
        charging.put(deviceId, false);
    }
}
