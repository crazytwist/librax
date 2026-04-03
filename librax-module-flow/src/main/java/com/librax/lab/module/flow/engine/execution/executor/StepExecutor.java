
package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.enums.StepTypeEnum;

import java.util.Map;

/**
 * 步骤执行器接口
 *
 * <p>职责：接收节点定义和运行时参数，执行具体业务逻辑，返回执行结果。
 *
 * <p>实现类列表：
 * <ul>
 *   <li>{@link MockStepExecutor}       - Mock执行器，开发测试用，返回预设输出
 *   <li>{@link ConditionStepExecutor}  - 条件执行器，Aviator表达式求值，决定分支走向
 *   <li>InstrumentStepExecutor         - 仪器执行器，发MQ指令，异步等设备回调
 *   <li>ComputeStepExecutor            - 计算执行器，调Spring Bean或LiteFlow Chain
 *   <li>WaitStepExecutor               - 等待执行器，等固定时长或外部信号
 *   <li>NotifyStepExecutor             - 通知执行器，发钉钉/企微/邮件
 *   <li>SampleSplitStepExecutor        - 样本拆分执行器，产生子样本记录
 * </ul>
 *
 * <p>注意：
 * <ul>
 *   <li>同步执行器（COMPUTE/CONDITION/WAIT/NOTIFY）在 execute() 里直接返回结果
 *   <li>异步执行器（INSTRUMENT）在 execute() 里只做指令下发，结果通过MQ回调
 *   <li>执行器必须保证幂等性，同一步骤重试时会再次调用
 *   <li>supportType() 返回 null 的执行器（如MockStepExecutor）不参与工厂自动注册
 * </ul>
 */
public interface StepExecutor {

    /**
     * 返回此执行器支持的步骤类型
     * <p>工厂通过此方法自动注册，新增执行器无需修改工厂代码
     * <p>返回 null 表示不参与工厂注册（如 MockStepExecutor）
     *
     * @return 步骤类型枚举，或 null
     */
    StepTypeEnum supportType();

    /**
     * 执行步骤
     *
     * @param node        步骤节点定义（含设备类型、执行器配置、超时重试策略等）
     * @param executionId 流程执行实例ID（日志追踪、设备回调匹配用）
     * @param inputParams 运行时入参（静态params + inputMapping解析结果合并后的最终值）
     * @return 执行结果：
     *         成功 → {@code StepResult.ok(outputs)}，outputs 写入上下文供后续节点引用；
     *         失败 → {@code StepResult.fail(errorCode, errorMsg)}，触发重试或DEAD流转
     */
    StepResult execute(StepNode node, String executionId, Map<String, Object> inputParams);
}