// flow-api 模块
package com.librax.lab.module.flow.api.statemachine;

import com.librax.lab.module.flow.api.enums.WaitingForEnum;

/**
 * 步骤状态 SPI
 * flow 模块实现,外部模块(task/device)通过此接口操作步骤状态
 * 只暴露外部模块真正需要的方法,不暴露引擎内部状态机
 */
public interface StepStateApi {
    /**
     * 步骤进入等待状态(等外部回调)
     * QUEUED 路径任务入队后调用
     */
    void markWaiting(String executionId, String nodeId, int attempt,
                     WaitingForEnum waitingFor, String callbackToken);
}