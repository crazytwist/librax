package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.engine.execution.callback.StepCallbackService;
import com.librax.lab.module.flow.service.pipelineexecution.PipelineExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 执行单元结果上报器
 * 用途：子流程（执行单元）的最后一个节点
 *       判断结果合格与否，回调父流程
 *
 * input_mapping 配置示例（子流程末尾节点）：
 * {
 *   "parentCallbackToken": "${input.parentCallbackToken}",
 *   "qualified":           "${s_compute.qualified}",
 *   "currentRetry":        "${input.currentRetry}",
 *   "maxRetry":            "${input.maxRetry}"
 * }
 */
@Slf4j
@Component("unitReporterBean")
@RequiredArgsConstructor
public class UnitReporterBean implements StepExecutor {

    private final StepCallbackService callbackService;
    private final PipelineExecutionService executionService;

    @Override
    public StepTypeEnum supportType() {
        return null;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        Map<String, Object> params = ctx.getInputParams();

        String  parentCallbackToken = getString(params, "parentCallbackToken");
        boolean qualified           = getBool(params,   "qualified");
        int     currentRetry        = getInt(params,    "currentRetry", 0);
        int     maxRetry            = getInt(params,    "maxRetry",     3);

        if (parentCallbackToken == null) {
            log.warn("[UnitReporter] parentCallbackToken 为空，子流程独立运行无需回调");
            return StepResult.ok(Map.of("reported", false));
        }

        if (qualified) {
            // ✅ 合格：通知父流程成功，携带本次检测结果
            log.info("[UnitReporter] 执行单元合格，回调父流程成功 token={} retry={}/{}",
                    parentCallbackToken, currentRetry + 1, maxRetry);

            callbackService.callbackByToken(
                    parentCallbackToken,
                    true,
                    collectResults(params),   // 把检测结果带回给父流程
                    null, null);

        } else {
            // ❌ 不合格：通知父流程，由父流程决定是否继续循环
            log.info("[UnitReporter] 执行单元不合格，回调父流程 retry={}/{} token={}",
                    currentRetry + 1, maxRetry, parentCallbackToken);

            callbackService.callbackByToken(
                    parentCallbackToken,
                    false,
                    null,
                    "UNIT_NOT_QUALIFIED",
                    String.format("第%d次执行不合格", currentRetry + 1));
        }

        return StepResult.ok(Map.of(
                "reported",     true,
                "qualified",    qualified,
                "currentRetry", currentRetry
        ));
    }

    /** 收集需要透传给父流程的结果字段 */
    private Map<String, Object> collectResults(Map<String, Object> params) {
        // 过滤掉系统字段，其余全部透传给父流程
        Set<String> systemKeys = Set.of(
                "parentCallbackToken", "currentRetry", "maxRetry",
                "unitPipelineKey", "unitPipelineVersion",
                "_resourceId"
        );
        Map<String, Object> results = new HashMap<>();
        params.forEach((k, v) -> {
            if (!systemKeys.contains(k) && v != null) {
                results.put(k, v);
            }
        });
        return results;
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private boolean getBool(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    private int getInt(Map<String, Object> params, String key, int defaultVal) {
        Object v = params.get(key);
        if (v == null) return defaultVal;
        return ((Number) v).intValue();
    }
}