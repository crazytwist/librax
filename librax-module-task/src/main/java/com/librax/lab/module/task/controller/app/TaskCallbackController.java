package com.librax.lab.module.task.controller.app;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.task.controller.app.vo.AgvCallbackReqVO;
import com.librax.lab.module.task.controller.app.vo.ManualCallbackReqVO;
import com.librax.lab.module.task.dispatch.TaskCallbackDispatcher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "任务步骤回调")
@RestController
@RequestMapping("/task/callback")
@RequiredArgsConstructor
public class TaskCallbackController {

    private final TaskCallbackDispatcher dispatcher;

    @PostMapping("/agv")
    @Operation(summary = "AGV 任务回调")
    public CommonResult<Boolean> agvCallback(@RequestBody @Valid AgvCallbackReqVO req) {
        boolean success = "SUCCESS".equalsIgnoreCase(req.getStatus());
        dispatcher.dispatch(
                req.getCallbackToken(),
                success,
                req.getOutputs(),
                success ? null : req.getErrorCode(),
                success ? null : req.getErrorMsg()
        );
        return CommonResult.success(true);
    }

    @PostMapping("/manual")
    @Operation(summary = "人工任务回调")
    public CommonResult<Boolean> manualCallback(@RequestBody @Valid ManualCallbackReqVO req) {
        dispatcher.dispatch(
                req.getCallbackToken(),
                Boolean.TRUE.equals(req.getApproved()),
                req.getFormData(),
                Boolean.TRUE.equals(req.getApproved()) ? null : "MANUAL_REJECTED",
                Boolean.TRUE.equals(req.getApproved()) ? null : req.getRejectReason()
        );
        return CommonResult.success(true);
    }

}
