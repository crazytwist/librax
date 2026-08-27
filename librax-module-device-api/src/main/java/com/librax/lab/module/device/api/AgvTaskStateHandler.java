package com.librax.lab.module.device.api;

/** AGV 子任务状态回调扩展点。返回 true 表示已由业务协调器消费。 */
public interface AgvTaskStateHandler {

    boolean handle(String taskId, String runState, String agvId);
}
