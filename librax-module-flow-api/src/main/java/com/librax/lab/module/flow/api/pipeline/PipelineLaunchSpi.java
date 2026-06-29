package com.librax.lab.module.flow.api.pipeline;

import java.util.Map;

/**
 * 流程启动 SPI
 *
 * <p>供 lab 等业务模块调用，屏蔽 flow 模块内部的 triggerType/triggeredBy 等基础设施细节。
 * flow 模块实现此接口，业务模块只需依赖 flow-api。
 */
public interface PipelineLaunchSpi {

    /**
     * 启动一条顶层流程（MANUAL 触发），使用流程定义中的 defaultInputParams
     *
     * @param pipelineKey 流程唯一标识
     * @return executionId
     */
    default String launch(String pipelineKey) {
        return launch(pipelineKey, null);
    }

    /**
     * 启动一条顶层流程（MANUAL 触发）
     *
     * @param pipelineKey 流程唯一标识
     * @param inputParams 初始参数，覆盖 defaultInputParams，节点可通过 ${input.xxx} 引用
     * @return executionId
     */
    String launch(String pipelineKey, Map<String, Object> inputParams);

    /**
     * 启动一条与当前流程解耦的独立子流程（fire-and-forget），使用流程定义中的 defaultInputParams
     *
     * @param pipelineKey 流程唯一标识
     * @return executionId
     */
    default String launchDetached(String pipelineKey) {
        return launchDetached(pipelineKey, null);
    }

    /**
     * 启动一条与当前流程解耦的独立子流程（fire-and-forget）
     *
     * <p>与 UNIT_LAUNCHER 的父子关系不同，此方法启动的流程完全独立，
     * 父流程不等待、不感知子流程结果，适用于"回调时分裂出独立下游流程"的场景。
     *
     * @param pipelineKey 流程唯一标识
     * @param inputParams 初始参数，覆盖 defaultInputParams
     * @return executionId
     */
    String launchDetached(String pipelineKey, Map<String, Object> inputParams);
}
