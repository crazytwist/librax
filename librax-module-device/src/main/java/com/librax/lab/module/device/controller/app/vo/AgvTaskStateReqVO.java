package com.librax.lab.module.device.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgvTaskStateReqVO {

    @NotBlank(message = "taskId不能为空")
    private String taskId;

    /**
     * 1=RUNNING, 2=DONE, 255=ERROR。
     */
    @NotBlank(message = "runState不能为空")
    private String runState;


    private String agvId;

    /**
     * 可选。若 taskId 不是 executionId_nodeId 格式，可显式传入。
     */
    private String executionId;

    /**
     * 可选。若 taskId 不是 executionId_nodeId 格式，可显式传入。
     */
    private String nodeId;

    /**
     * 可选。真实设备建议透传下发时携带的 callbackToken。
     */
    private String callbackToken;
}
