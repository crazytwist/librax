package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.enums.StepTypeEnum;
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
    public StepResult execute(StepNode node,
                              String executionId,
                              Map<String, Object> inputParams) {
        String executor = node.getExecutor();
        log.info("[ComputeExecutor] 开始执行 executionId={} nodeId={} executor={}",
                executionId, node.getNodeId(), executor);

        try {
            Map<String, Object> outputs;
            switch (executor) {
                case "BEAN":
                    outputs = executeBean(node, executionId, inputParams);
                    break;
                case "LITEFLOW":
                    outputs = executeLiteFlow(node, executionId, inputParams);
                    break;
                default:
                    return StepResult.fail("COMPUTE_ERROR",
                            "不支持的 executor 类型: " + executor);
            }

            log.info("[ComputeExecutor] 执行成功 executionId={} nodeId={} outputKeys={}",
                    executionId, node.getNodeId(), outputs.keySet());
            return StepResult.ok(outputs);

        } catch (Exception e) {
            log.error("[ComputeExecutor] 执行异常 executionId={} nodeId={} executor={} error={}",
                    executionId, node.getNodeId(), executor, e.getMessage(), e);
            return StepResult.fail("COMPUTE_ERROR",
                    "计算节点执行失败: " + e.getMessage());
        }
    }

    // ================================================================
    // BEAN 模式
    // ================================================================

    /**
     * 反射调用 Spring Bean 方法
     *
     * <p>方法签名约定：
     * {@code Map<String, Object> methodName(Map<String, Object> inputParams)}
     *
     * @param node        节点定义（含 beanName、methodName）
     * @param executionId 执行实例ID（日志用）
     * @param inputParams 运行时入参
     * @return 方法返回的 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeBean(StepNode node,
                                            String executionId,
                                            Map<String, Object> inputParams) throws Exception {
        String beanName = node.getBeanName();
        String methodName = node.getMethodName();

        // 1. 从 Spring 容器获取 Bean
        Object bean = applicationContext.getBean(beanName);

        // 2. 查找方法（统一签名：Map 入参，Map 返回）
        Method method = bean.getClass().getMethod(methodName, Map.class);

        log.info("[ComputeExecutor] BEAN调用 executionId={} bean={}.{}() params={}",
                executionId, beanName, methodName, inputParams.keySet());

        // 3. 反射调用
        Object result = method.invoke(bean, inputParams);

        // 4. 返回值处理
        if (result == null) {
            return new HashMap<>();
        }
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        // 非 Map 返回值，包装成 {"result": xxx}
        return Map.of("result", result);
    }

    // ================================================================
    // LITEFLOW 模式
    // ================================================================

    /**
     * 调用 LiteFlow Chain
     *
     * <p>inputParams 通过 requestData 传入 Chain，
     * Chain 内的组件通过 {@code this.getRequestData()} 获取。
     *
     * <p>输出约定：
     * <ul>
     *   <li>Chain 中的组件通过 {@code this.getContextBean(ComputeContext.class)}
     *       把结果写入上下文
     *   <li>执行完毕后从 LiteflowResponse 的 contextBean 中提取输出
     *   <li>如果没有 ComputeContext，尝试从 response 的 slot 数据中提取
     * </ul>
     *
     * @param node        节点定义（含 chainId）
     * @param executionId 执行实例ID
     * @param inputParams 运行时入参
     * @return Chain 执行后的输出 Map
     */
    private Map<String, Object> executeLiteFlow(StepNode node,
                                                String executionId,
                                                Map<String, Object> inputParams) {
        String chainId = node.getChainId();

        log.info("[ComputeExecutor] LITEFLOW调用 executionId={} chainId={} params={}",
                executionId, chainId, inputParams.keySet());

        // 1. 创建上下文对象，把 inputParams 放进去
        ComputeContext context = new ComputeContext();
        context.setInputParams(inputParams);

        // 2. 执行 Chain
        LiteflowResponse response = flowExecutor.execute2Resp(
                chainId, null, context);

        // 3. 检查执行结果
        if (!response.isSuccess()) {
            Exception cause = response.getCause();
            String errorMsg = cause != null ? cause.getMessage() : "LiteFlow Chain 执行失败";
            throw new RuntimeException(errorMsg, cause);
        }

        // 4. 从上下文提取输出
        ComputeContext resultContext = response.getContextBean(ComputeContext.class);
        if (resultContext != null && resultContext.getOutputs() != null) {
            return resultContext.getOutputs();
        }

        log.warn("[ComputeExecutor] LiteFlow Chain 无输出 chainId={}", chainId);
        return new HashMap<>();
    }
}