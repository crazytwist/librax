
package com.librax.lab.module.flow.engine.execution.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 重试策略注册表
 *
 * 根据 errorCode 匹配策略，决定是否重试、重试参数、是否告警。
 * 匹配规则：按注册顺序，第一个匹配的生效（前缀匹配）。
 * 没有匹配到的走默认策略（可重试）。
 *
 * 后续可改为从 DB 表加载，支持动态配置。
 */
@Slf4j
@Component
public class RetryPolicyRegistry {

    private final List<RetryPolicy> policies = new ArrayList<>();

    @PostConstruct
    public void init() {
        // ---- 不可重试的错误（配置问题、代码 bug）----
        register(RetryPolicy.builder()
                .errorCodePattern("CONDITION_EVAL_FAIL")
                .retryable(false)
                .alertOnFail(true).alertLevel("CRITICAL")
                .description("条件表达式求值失败，配置问题，不重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("CONDITION_NO_MATCH")
                .retryable(false)
                .alertOnFail(true).alertLevel("CRITICAL")
                .description("条件分支无匹配，配置问题，不重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("PARAM_MISSING")
                .retryable(false)
                .alertOnFail(true).alertLevel("WARNING")
                .description("参数缺失，配置问题，不重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("COMPUTE_ERROR")
                .retryable(false)
                .alertOnFail(true).alertLevel("CRITICAL")
                .description("计算逻辑错误，代码问题，不重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("CONDITION_NOT_BOOLEAN")
                .retryable(false)
                .alertOnFail(true).alertLevel("CRITICAL")
                .description("条件表达式返回非布尔值，配置问题，不重试")
                .build());

        // ---- 可重试的错误（临时性故障）----
        register(RetryPolicy.builder()
                .errorCodePattern("STEP_TIMEOUT")
                .retryable(true)
                .alertOnFail(true).alertLevel("WARNING")
                .description("步骤超时，可能临时忙，重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("DEVICE_TIMEOUT")
                .retryable(true)
                .alertOnFail(true).alertLevel("WARNING")
                .description("设备超时，网络可能抖动，重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("DEVICE_OFFLINE")
                .retryable(true)
                .backoffMs(10_000L)  // 设备离线退避久一点
                .alertOnFail(true).alertLevel("CRITICAL")
                .description("设备离线，重试+告警")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("DEVICE_")
                .retryable(true)
                .alertOnFail(true).alertLevel("WARNING")
                .description("设备类错误，默认重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("NETWORK_")
                .retryable(true)
                .description("网络类错误，重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("CALLBACK_FAIL")
                .retryable(true)
                .description("回调报告失败，重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("EXECUTE_EXCEPTION")
                .retryable(true)
                .alertOnFail(true).alertLevel("WARNING")
                .description("执行器异常，可能临时问题，重试")
                .build());

        // ---- 业务类错误（不重试）----
        register(RetryPolicy.builder()
                .errorCodePattern("BIZ_")
                .retryable(false)
                .alertOnFail(false)
                .description("业务拒绝，不重试")
                .build());

        register(RetryPolicy.builder()
                .errorCodePattern("EXECUTION_TIMEOUT")
                .retryable(false)
                .alertOnFail(true).alertLevel("CRITICAL")
                .description("流程整体超时，不重试")
                .build());

        log.info("[RetryPolicyRegistry] 注册 {} 条重试策略", policies.size());
    }

    public void register(RetryPolicy policy) {
        policies.add(policy);
    }

    /**
     * 根据 errorCode 匹配策略
     * 按注册顺序匹配，第一个匹配的生效（前缀匹配）
     */
    public RetryPolicy resolve(String errorCode) {
        if (errorCode == null) return defaultPolicy();

        for (RetryPolicy policy : policies) {
            if (matches(errorCode, policy.getErrorCodePattern())) {
                return policy;
            }
        }
        return defaultPolicy();
    }

    /**
     * 默认策略：可重试，不告警
     */
    private RetryPolicy defaultPolicy() {
        return RetryPolicy.builder()
                .errorCodePattern("*")
                .retryable(true)
                .alertOnFail(false)
                .description("默认策略：可重试")
                .build();
    }

    /**
     * 前缀匹配：DEVICE_ 匹配 DEVICE_TIMEOUT、DEVICE_OFFLINE 等
     */
    private boolean matches(String errorCode, String pattern) {
        if (pattern.endsWith("_")) {
            return errorCode.startsWith(pattern);
        }
        return errorCode.equals(pattern);
    }
}
