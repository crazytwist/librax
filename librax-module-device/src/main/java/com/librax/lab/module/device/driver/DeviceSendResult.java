package com.librax.lab.module.device.driver;

import lombok.Data;

/**
 * 设备指令发送结果
 *
 * <p>封装驱动层发送结果，供网关层判断是否同步完成。
 */
@Data
public class DeviceSendResult {

    /** 设备侧任务 ID */
    private String taskId;

    /** 原始响应体（同步完成模式下用于提取输出数据） */
    private String responseBody;

    /**
     * 是否同步完成（completionMode=SYNC 时由网关层设为 true）。
     * true 表示 HTTP 响应返回即代表指令执行完毕，调用方可直接推进下一节点，无需等待设备回调。
     */
    private boolean syncCompleted = false;

    public static DeviceSendResult of(String taskId, String responseBody) {
        DeviceSendResult r = new DeviceSendResult();
        r.taskId = taskId;
        r.responseBody = responseBody;
        return r;
    }
}
