package com.librax.lab.module.flow.api.dispatch;

/**
 * 步骤分发 SPI
 * <p>
 * flow 模块只调此接口，不感知 device/task 的任何实现细节。
 * device 模块实现 DIRECT，task 模块实现 QUEUED。
 * <p>
 * Spring 自动发现所有实现类，StepSubmitter 按 dispatch_mode 路由。
 */
public interface DispatchSpi {

    /**
     * 支持的调度模式，对应 pd_pipeline_step.dispatch_mode
     *
     * @return "DIRECT" 或 "QUEUED"
     */
    String supportMode();

    /**
     * 执行分发
     * 实现类负责：申请资源 → 执行 → 等回调（或入队）
     * 不需要关心 DAG 调度的后续逻辑，通过 ctx.callback 通知引擎
     *
     * @param ctx 步骤分发上下文，携带本次分发所需的全部信息
     */
    void dispatch(StepDispatchContext ctx);
}
