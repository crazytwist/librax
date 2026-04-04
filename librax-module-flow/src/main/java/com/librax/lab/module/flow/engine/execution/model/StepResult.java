package com.librax.lab.module.flow.engine.execution.model;


import com.librax.lab.module.flow.enums.WaitingForEnum;
import lombok.Data;

import java.util.Map;

/**
 * 步骤执行结果
 * <p>执行器执行完成后返回此对象，由调度器根据 success 决定后续流转
 *
 * @author yinan
 */
@Data
public class StepResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 步骤输出数据，写入 ExecutionContext 供后续步骤引用
     */
    private Map<String, Object> outputs;

    /**
     * 错误码（失败时填写）
     */
    private String errorCode;

    /**
     * 错误详情（失败时填写）
     */
    private String errorMsg;

    /**
     * 是否等待
     */
    private boolean waiting = false;

    /**
     * 等待类型枚举
     */
    private WaitingForEnum waitingFor;

    /**
     * 异步等待 — 执行器告诉调度器"我发完指令了，等外部回调"
     *
     * @param waitingFor 等待类型
     * @param outputs    中间数据（如设备任务ID），写入上下文供回调时校验
     */
    public static StepResult waiting(WaitingForEnum waitingFor,
                                     Map<String, Object> outputs) {
        StepResult r = new StepResult();
        r.setSuccess(false);
        r.setWaiting(true);
        r.setWaitingFor(waitingFor);
        r.setOutputs(outputs);
        return r;
    }

    // 快捷方法
    public static StepResult waitForDevice(Map<String, Object> outputs) {
        return waiting(WaitingForEnum.DEVICE_CALLBACK, outputs);
    }

    public static StepResult waitForApproval(Map<String, Object> outputs) {
        return waiting(WaitingForEnum.MANUAL_APPROVE, outputs);
    }

    // ---- 静态工厂方法 ----

    public static StepResult ok(Map<String, Object> outputs) {
        StepResult r = new StepResult();
        r.success = true;
        r.outputs = outputs;
        return r;
    }

    public static StepResult ok() {
        return ok(Map.of());
    }

    public static StepResult fail(String errorCode, String errorMsg) {
        StepResult r = new StepResult();
        r.success = false;
        r.errorCode = errorCode;
        r.errorMsg = errorMsg;
        return r;
    }

    public static StepResult fail(String errorMsg) {
        return fail("UNKNOWN_ERROR", errorMsg);
    }
}