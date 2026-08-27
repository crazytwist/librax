package com.librax.lab.module.device.api;

/**
 * AGV 下料任务完成装载（step1）时的业务扩展点。
 *
 * <p>taskId 是下料编排任务对外暴露的流程 executionId。处理器异常不会中断
 * AGV 后续的移动、卸料和仓储入库流程。
 */
public interface AgvReturnLoadCompleteHandler {

    /**
     * @param taskId 下料编排任务的 executionId
     */
    void onLoadComplete(String taskId);
}
