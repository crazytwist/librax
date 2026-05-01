package com.librax.lab.module.flow.controller.app;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.flow.controller.app.vo.ApproveReqVO;
import com.librax.lab.module.flow.controller.app.vo.StepCallbackReqVO;
import com.librax.lab.module.flow.engine.execution.callback.CallbackResult;
import com.librax.lab.module.flow.engine.execution.callback.StepCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "流程步骤回调")
@RestController
@RequestMapping("/app-api/flow/callback")
@RequiredArgsConstructor
public class StepCallbackController {

    private final StepCallbackService callbackService;

    /**
     * 设备/外部系统回调 — 推进步骤完成
     * <p>
     * 调用示例（设备完成后）：
     * POST /app-api/flow/callback/step-complete
     * {
     * "executionId": "cd5d31d5389540db...",
     * "nodeId": "s_ph",
     * "callbackToken": "abc123...",
     * "success": true,
     * "outputs": {"ph": 7.2, "temperature": 25.0}
     * }
     */
    @PostMapping("/step-complete")
    @Operation(summary = "步骤回调 — 外部推进步骤完成")
    public CommonResult<Boolean> stepComplete(
            @Valid @RequestBody StepCallbackReqVO req) {

        CallbackResult result = callbackService.callback(
                req.getExecutionId(),
                req.getNodeId(),
                req.getCallbackToken(),
                req.isSuccess(),
                req.getOutputs(),
                req.getErrorCode(),
                req.getErrorMsg());

        if (result.isSuccess()) {
            return CommonResult.success(true);
        }
        return CommonResult.error(400, result.getErrorMsg());
    }

    /**
     * 人工审批 — 推进步骤（简化接口）
     * <p>
     * POST /app-api/flow/callback/approve
     * {
     * "executionId": "xxx",
     * "nodeId": "s_review",
     * "approved": true,
     * "comment": "数据合格"
     * }
     */
    @PostMapping("/approve")
    @Operation(summary = "人工审批 — 通过/驳回步骤")
    public CommonResult<Boolean> approve(
            @Valid @RequestBody ApproveReqVO req) {

        Map<String, Object> outputs = Map.of(
                "approved", req.isApproved(),
                "comment", req.getComment() != null ? req.getComment() : "",
                "approvedBy", req.getApprovedBy() != null ? req.getApprovedBy() : "UNKNOWN",
                "approvedAt", java.time.LocalDateTime.now().toString()
        );

        CallbackResult result = callbackService.callback(
                req.getExecutionId(),
                req.getNodeId(),
                req.getCallbackToken(),
                req.isApproved(),
                outputs,
                req.isApproved() ? null : "REJECTED",
                req.isApproved() ? null : "审批驳回: " + req.getComment());

        if (result.isSuccess()) {
            return CommonResult.success(true);
        }
        return CommonResult.error(400, result.getErrorMsg());
    }

}


