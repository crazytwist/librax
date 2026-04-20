package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 计算步骤执行器
 *
 * <p>支持两种执行模式：
 * <ul>
 *   <li><b>BEAN</b> — 反射调用 Spring Bean 的指定方法，方法签名统一约定为：
 *       {@code Map<String, Object> methodName(Map<String, Object> inputParams)}
 *   <li><b>LITEFLOW</b> — 调用 LiteFlow Chain，inputParams 通过 requestData 传入，
 *       Chain 执行完毕后从 contextBean 取输出
 * </ul>
 *
 * <p>配置来源（三层合并后写在 StepNode 里）：
 * <pre>
 *   pd_step_definition:
 *     executor = "BEAN"
 *     bean_name = "scoreCalculator"
 *     method_name = "calculate"
 *   或
 *     executor = "LITEFLOW"
 *     chain_id = "score_chain"
 * </pre>
 *
 * <p>幂等要求：同一步骤重试时会再次调用 execute()，
 * 具体的 Bean 方法和 LiteFlow Chain 必须保证幂等。
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class ComputeStepExecutor implements StepExecutor {

    private final ApplicationContext applicationContext;
    private final FlowExecutor flowExecutor;

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.COMPUTE;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        String executor = ctx.getExecutor();
        String executionId = ctx.getExecutionId();
        String nodeId = ctx.getNodeId();

        log.info("[ComputeExecutor] 开始执行 executionId={} nodeId={} executor={}",
                executionId, nodeId, executor);

        try {
            Map<String, Object> outputs = switch (executor) {
                case "BEAN" -> executeBean(ctx);
                case "LITEFLOW" -> executeLiteFlow(ctx);
                default -> throw new IllegalArgumentException(
                        "不支持的 executor 类型: " + executor);
            };

            log.info("[ComputeExecutor] 执行成功 executionId={} nodeId={} outputKeys={}",
                    executionId, nodeId, outputs.keySet());
            return StepResult.ok(outputs);

        } catch (Exception e) {
            log.error("[ComputeExecutor] 执行异常 executionId={} nodeId={} executor={} error={}",
                    executionId, nodeId, executor, e.getMessage(), e);
            return StepResult.fail("COMPUTE_ERROR", "计算节点执行失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeBean(StepDispatchContext ctx) throws Exception {
        Object bean = applicationContext.getBean(ctx.getBeanName());
        Method method = bean.getClass().getMethod(ctx.getMethodName(), Map.class);

        log.info("[ComputeExecutor] BEAN调用 executionId={} bean={}.{}() params={}",
                ctx.getExecutionId(), ctx.getBeanName(),
                ctx.getMethodName(), ctx.getInputParams().keySet());

        Object result = method.invoke(bean, ctx.getInputParams());
        if (result == null) return new HashMap<>();
        if (result instanceof Map) return (Map<String, Object>) result;
        return Map.of("result", result);
    }

    private Map<String, Object> executeLiteFlow(StepDispatchContext ctx) {
        log.info("[ComputeExecutor] LITEFLOW调用 executionId={} chainId={} params={}",
                ctx.getExecutionId(), ctx.getChainId(), ctx.getInputParams().keySet());

        ComputeContext context = new ComputeContext();
        context.setInputParams(ctx.getInputParams());

        LiteflowResponse response = flowExecutor.execute2Resp(
                ctx.getChainId(), null, context);

        if (!response.isSuccess()) {
            Exception cause = response.getCause();
            throw new RuntimeException(
                    cause != null ? cause.getMessage() : "LiteFlow Chain 执行失败", cause);
        }

        ComputeContext resultCtx = response.getContextBean(ComputeContext.class);
        if (resultCtx != null && resultCtx.getOutputs() != null) {
            return resultCtx.getOutputs();
        }
        log.warn("[ComputeExecutor] LiteFlow Chain 无输出 chainId={}", ctx.getChainId());
        return new HashMap<>();
    }
}
