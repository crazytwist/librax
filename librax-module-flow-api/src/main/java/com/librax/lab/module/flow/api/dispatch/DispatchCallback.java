package com.librax.lab.module.flow.api.dispatch;

import java.util.Map;

/**
 * 步骤完成回调
 *
 * 实现类执行完步骤后调用，通知 flow 引擎继续 DAG 调度。
 * 由 StepSubmitter 实现并通过 StepDispatchContext 传入。
 */
@FunctionalInterface
public interface DispatchCallback {

    /**
     * @param executionId 执行实例ID
     * @param nodeId      节点ID
     * @param attempt     本次尝试次数
     * @param success     是否成功
     * @param outputs     成功时的输出数据
     * @param errorCode   失败时的错误码
     * @param errorMsg    失败时的错误信息
     */
    void onComplete(String executionId,
                    String nodeId,
                    int attempt,
                    boolean success,
                    Map<String, Object> outputs,
                    String errorCode,
                    String errorMsg);
}