package com.librax.lab.module.flow.api.callback;

import java.util.Map;

/**
 * 任务关闭 SPI
 *
 * <p>供设备回调路径（{@code DeviceCallbackHandler}）在推进 DAG 后调用，
 * 通过 callbackToken 关闭 QUEUED 路径下 INSTRUMENT 步骤创建的 {@code lab_task} 记录。
 *
 * <p>设计目的：打通 device → task 的回调链路，避免 QUEUED+ASYNC 场景下
 * task 记录长期停留 EXECUTING，被 Watchdog 误判超时并重新入队导致指令重复发送。
 *
 * <p>实现方由 task 模块提供；device 模块通过 SPI 调用，不产生直接依赖。
 * 若 task 模块未部署（如单元测试环境），注入为 null 时调用方静默跳过。
 */
public interface TaskCallbackSpi {

    /**
     * 通过 callbackToken 关闭对应的任务记录（终态 DONE 或 FAILED）。
     *
     * <p>若找不到对应 task（DIRECT 路径无 task 记录），静默忽略，不抛异常。
     *
     * @param callbackToken 回调令牌，与 pe_step_execution.callback_token 一致
     * @param success       执行是否成功
     * @param outputs       任务产出（成功时）
     * @param errorCode     错误码（失败时）
     * @param errorMsg      错误信息（失败时）
     */
    void closeByToken(String callbackToken,
                      boolean success,
                      Map<String, Object> outputs,
                      String errorCode,
                      String errorMsg);
}
