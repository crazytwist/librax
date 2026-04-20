
package com.librax.lab.module.flow.api.executor;


import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.model.StepResult;

import java.util.Map;

/**
 * 步骤执行器接口
 *
 * <p>职责：接收节点定义和运行时参数，执行具体业务逻辑，返回执行结果。
 *
 * <p>实现类列表：
 * <ul>
 *   <li>{MockStepExecutor}       - Mock执行器，开发测试用，返回预设输出
 *   <li>{ConditionStepExecutor}  - 条件执行器，Aviator表达式求值，决定分支走向
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
     * @param ctx   分发上下文（替换原来的 StepNode + executionId + inputParams 三个参数）
     * @return 执行结果
     */
    StepResult execute(StepDispatchContext ctx);
}