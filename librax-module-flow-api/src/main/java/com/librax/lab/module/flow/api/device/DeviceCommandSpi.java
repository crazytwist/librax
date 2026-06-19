package com.librax.lab.module.flow.api.device;

import lombok.Data;

import java.util.Map;

/**
 * 设备指令发送 SPI
 *
 * <p>供 task 模块（{@code InstrumentTaskExecutor}）在 QUEUED 路径下
 * 向设备发送指令，而无需直接依赖 device 模块。
 *
 * <p>实现方由 device 模块提供，task 模块通过此 SPI 调用，不产生直接依赖。
 */
public interface DeviceCommandSpi {

    /**
     * 向设备发送指令
     *
     * @param deviceType    设备类型，如 PH_METER
     * @param commandCode   指令代码，如 MEASURE
     * @param params        运行时参数，替换模板中的 ${xxx} 占位符
     * @param executionId   流程执行 ID（透传给设备，回调时带回用于匹配步骤）
     * @param nodeId        节点 ID（透传给设备，回调时带回）
     * @param callbackToken 回调令牌（透传给设备，校验用）
     * @return 发送结果；{@link Result#isSyncCompleted()} 为 true 时调用方可立即视为成功
     */
    Result send(String deviceType,
                String commandCode,
                Map<String, Object> params,
                String executionId,
                String nodeId,
                String callbackToken);

    /**
     * 设备指令发送结果
     */
    @Data
    class Result {

        /** 设备侧任务 ID */
        private String taskId;

        /**
         * 是否同步完成（completionMode=SYNC 时为 true）。
         * true 表示 HTTP 响应返回即代表完成，无需等待设备回调。
         */
        private boolean syncCompleted;

        /** 原始响应体（syncCompleted=true 时可从此提取输出数据） */
        private String responseBody;

        public static Result async(String taskId) {
            Result r = new Result();
            r.taskId = taskId;
            r.syncCompleted = false;
            return r;
        }

        public static Result sync(String taskId, String responseBody) {
            Result r = new Result();
            r.taskId = taskId;
            r.syncCompleted = true;
            r.responseBody = responseBody;
            return r;
        }
    }
}
