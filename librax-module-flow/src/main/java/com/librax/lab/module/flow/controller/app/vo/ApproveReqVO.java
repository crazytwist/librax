package com.librax.lab.module.flow.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApproveReqVO {
    @NotBlank(message = "executionId不能为空")
    private String executionId;
    @NotBlank(message = "nodeId不能为空")
    private String nodeId;
    private String callbackToken;
    private boolean approved;
    private String comment;
    private String approvedBy;
}