package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.enums.WaitingForEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.service.pipelineexecution.PipelineExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行单元启动器
 * 用途：父流程里的节点，启动一个子流程（执行单元）并等待其完成
 *
 * params_override 配置示例：
 * {
 *   "unitPipelineKey":     "water_quality_unit",  // 子流程 key
 *   "unitPipelineVersion": 1,                     // 子流程版本，null 取最新
 *   "maxRetry":            3                       // 最大循环次数
 * }
 */
@Slf4j
@Component("unitLauncherBean")
@RequiredArgsConstructor
public class UnitLauncherBean implements StepExecutor {

    private final ApplicationContext applicationContext;

    private PipelineExecutionService getExecutionService() {
        return applicationContext.getBean(PipelineExecutionService.class);
    }

    @Override
    public StepTypeEnum supportType() {
        return null;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        Map<String, Object> params = ctx.getInputParams();

        // 从配置取子流程信息
        String unitPipelineKey = getString(params, "unitPipelineKey");
        Integer unitPipelineVersion = getInt(params, "unitPipelineVersion");
        int maxRetry     = getInt(params, "maxRetry",     3);
        int currentRetry = getInt(params, "currentRetry", 0);

        if (unitPipelineKey == null) {
            return StepResult.fail("UNIT_CONFIG_MISSING",
                    "unitPipelineKey 未配置");
        }

        // 检查是否超过最大循环次数
        if (currentRetry >= maxRetry) {
            log.warn("[UnitLauncher] 执行单元超过最大循环次数 " +
                            "executionId={} nodeId={} maxRetry={}",
                    ctx.getExecutionId(), ctx.getNodeId(), maxRetry);
            return StepResult.fail("UNIT_MAX_RETRY_EXCEEDED",
                    String.format("执行单元循环次数已达上限 %d 次", maxRetry));
        }

        // 把父流程的业务参数传给子流程
        // 同时传入父流程 callbackToken 和循环计数
        Map<String, Object> childParams = new HashMap<>(params);
        childParams.put("parentCallbackToken", ctx.getCallbackToken());
        childParams.put("currentRetry", currentRetry);
        childParams.put("maxRetry",     maxRetry);

        PipelineExecutionService executionService = getExecutionService();
        // 启动子流程
        String childExecutionId = executionService.startChild(
                unitPipelineKey,
                unitPipelineVersion,
                ctx.getExecutionId(),
                ctx.getCallbackToken(),   // ★ 父流程等待节点的 token
                childParams);

        log.info("[UnitLauncher] 子流程已启动 " +
                        "parentExecutionId={} childExecutionId={} retry={}/{}",
                ctx.getExecutionId(), childExecutionId,
                currentRetry + 1, maxRetry);

        // 返回 WAITING，父流程等待子流程回调
        return StepResult.waiting(WaitingForEnum.CHILD_EXECUTION, Map.of(
                "childExecutionId", childExecutionId,
                "unitPipelineKey",  unitPipelineKey,
                "currentRetry",     currentRetry,
                "maxRetry",         maxRetry
        ));
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private int getInt(Map<String, Object> params, String key, int defaultVal) {
        Object v = params.get(key);
        if (v == null) return defaultVal;
        return ((Number) v).intValue();
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? ((Number) v).intValue() : null;
    }
}