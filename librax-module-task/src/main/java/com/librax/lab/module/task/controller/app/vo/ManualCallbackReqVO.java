package com.librax.lab.module.task.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 人工任务回调请求 VO
 * <p>
 * 前端操作人员在待办页面确认/驳回人工任务后,调此接口推进流程。
 * 典型场景:人工审批、人工复核、需要操作人填表的检测步骤。
 */
@Schema(description = "人工任务回调请求")
@Data
public class ManualCallbackReqVO {

    @Schema(description = "回调令牌", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "a1b2c3d4e5f6")
    @NotBlank(message = "回调令牌不能为空")
    private String callbackToken;

    @Schema(description = "是否审批通过", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "true")
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Schema(description = "表单数据,approved=true 时必填,作为任务产出写入流程上下文",
            example = "{\"reviewer\":\"张三\",\"comment\":\"数据正常\",\"score\":95}")
    private Map<String, Object> formData;

    @Schema(description = "驳回原因,approved=false 时建议填写",
            example = "样本数据异常,需要重新检测")
    private String rejectReason;

    @Schema(description = "操作人,用于审计日志", example = "user_1001")
    private String operator;
}