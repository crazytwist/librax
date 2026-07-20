package com.librax.lab.module.device.api;

/**
 * AGV 等待信号扩展点。
 *
 * <p>设备模块只负责接收标准回调；多轮装载等业务状态由其它模块实现。
 * 返回 {@code true} 表示信号已被业务协调器消费，控制器不再推进默认流程 WAIT 节点。
 */
public interface AgvWaitSignalHandler {

    boolean handle(String taskId, String agvStation);
}
