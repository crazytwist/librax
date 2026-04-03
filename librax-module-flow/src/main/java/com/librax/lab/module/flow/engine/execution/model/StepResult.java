package com.librax.lab.module.flow.engine.execution.model;


import lombok.Data;

import java.util.Map;

/**
 * 步骤执行结果
 * <p>执行器执行完成后返回此对象，由调度器根据 success 决定后续流转
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