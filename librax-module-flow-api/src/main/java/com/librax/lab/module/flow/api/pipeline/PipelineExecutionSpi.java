package com.librax.lab.module.flow.api.pipeline;

public interface PipelineExecutionSpi {


    /**
     * 查询父级执行流程ID
     *
     * @param executionId 执行流程ID
     * @return 父级ID
     */
    String selectParentExecutionId(String executionId);

    /**
     * 流程执行是否仍处于 RUNNING 状态。
     *
     * <p>供跨模块的"排队重试"类场景使用（比如 AGV 编排请求排队等待槽位释放）：
     * 重试前先确认对应流程还活着，避免给一个已经被取消/超时/失败的流程继续执行有物理副作用的动作
     * （比如已经不需要了还去派AGV搬运）。
     *
     * @param executionId 执行流程ID
     * @return true 表示流程存在且状态为 RUNNING
     */
    boolean isRunning(String executionId);
}
