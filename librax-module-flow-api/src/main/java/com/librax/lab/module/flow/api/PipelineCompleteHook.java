package com.librax.lab.module.flow.api;

/**
 * 流程结束后置钩子 SPI（可选，供扩展）
 */
public interface PipelineCompleteHook {

    void afterComplete(String executionId, boolean success);

}