package com.librax.lab.module.device.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgvWaitSignalReqVO {

    @NotBlank(message = "taskId不能为空")
    private String taskId;

    /** 当前 AGV 位置，对应标准接口字段 agvStation。 */
    @NotBlank(message = "agvStation不能为空")
    private String agvStation;

    /**
     * 可选。若 taskId 不是 executionId_nodeId 格式，可显式传入。
     */
    private String executionId;

    /**
     * 可选。默认唤醒 AGV 子流程里的 s_agv_wait_signal。
     */
    private String nodeId;

    /**
     * 可选。真实设备建议透传下发时携带的 callbackToken。
     */
    private String callbackToken;
}
