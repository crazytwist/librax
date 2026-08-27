package com.librax.lab.module.flow.api.pipeline;

/**
 * 顶层流程执行结果监听扩展点。
 *
 * <p>业务模块通过该 SPI 关联异步子流程，无需依赖 flow 模块的内部事件类。</p>
 */
public interface PipelineExecutionStateHandler {

    /**
     * @return 已识别并处理该 executionId 时返回 true
     */
    boolean handle(String executionId, boolean success);
}
