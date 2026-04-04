package com.librax.lab.module.flow.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class StepCallbackReqVO {
    @NotBlank(message = "executionId不能为空")
    private String executionId;
    @NotBlank(message = "nodeId不能为空")
    private String nodeId;
    private String callbackToken;
    private boolean success;
    private Map<String, Object> outputs;
    private String errorCode;
    private String errorMsg;
}