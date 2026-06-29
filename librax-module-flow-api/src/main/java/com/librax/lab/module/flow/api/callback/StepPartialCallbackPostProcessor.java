package com.librax.lab.module.flow.api.callback;

import java.util.Map;

/**
 * 步骤部分回调后处理器 SPI
 *
 * <p>用于"一个步骤需要收到多次回调才算完成"的场景。
 * 典型场景：Machine A 批量上样后，每出一个样本回调一次，
 * 每次回调触发该样本的下游流程，全部出完后步骤才完成。
 *
 * <h3>使用方式</h3>
 * 在步骤参数中配置处理器 Bean 名称：
 * <pre>
 * params:
 *   partialCallbackProcessor: machineACallbackProcessor
 *   expectedCallbackCount: 4
 * </pre>
 *
 * <h3>执行时序</h3>
 * <ol>
 *   <li>每次回调到达时，引擎先校验 executionId / token</li>
 *   <li>校验通过后调用 {@link #onEachCallback}（业务侧在此触发下游流程）</li>
 *   <li>调用 {@link #isCompleted} 判断是否可以推进 DAG</li>
 *   <li>未完成：步骤保持 WAITING，等待下一次回调</li>
 *   <li>完成：走正常的步骤完成流程</li>
 * </ol>
 *
 * <h3>实现约束</h3>
 * <ul>
 *   <li>实现类需自行维护回调计数状态（推荐 Redis）</li>
 *   <li>{@link #onEachCallback} 应保证幂等</li>
 *   <li>实现类注册为 Spring Bean，名称与步骤参数中配置的一致</li>
 * </ul>
 */
public interface StepPartialCallbackPostProcessor {

    /**
     * 每次回调到达时触发
     *
     * <p>在此方法中执行业务动作，如为当前回调携带的样本启动下游流程。
     * 此方法被调用时步骤仍处于 WAITING 状态。
     *
     * @param executionId     当前步骤所属的执行实例 ID
     * @param nodeId          当前步骤的节点 ID
     * @param callbackPayload 本次回调携带的数据（如 sampleId、result 等）
     */
    void onEachCallback(String executionId, String nodeId, Map<String, Object> callbackPayload);

    /**
     * 判断步骤是否已满足完成条件
     *
     * <p>在 {@link #onEachCallback} 之后调用。
     * 返回 true 时引擎推进 DAG，步骤进入完成流程；
     * 返回 false 时步骤继续等待下一次回调。
     *
     * @param executionId 执行实例 ID
     * @param nodeId      节点 ID
     * @param stepParams  步骤的完整参数（即 inputSnapshot），可读取 expectedCallbackCount 等配置
     * @return true 表示所有回调已到齐，步骤可以完成
     */
    boolean isCompleted(String executionId, String nodeId, Map<String, Object> stepParams);
}
