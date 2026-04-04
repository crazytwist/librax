package com.librax.lab.module.flow.engine.execution.callback;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 回调结果
 */
@Data
@AllArgsConstructor
public class CallbackResult {
    private boolean success;
    private String errorCode;
    private String errorMsg;

    public static CallbackResult ok() {
        return new CallbackResult(true, null, null);
    }

    public static CallbackResult fail(String code, String msg) {
        return new CallbackResult(false, code, msg);
    }
}