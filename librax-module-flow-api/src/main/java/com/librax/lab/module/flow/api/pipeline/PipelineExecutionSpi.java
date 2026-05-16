package com.librax.lab.module.flow.api.pipeline;

public interface PipelineExecutionSpi {


    /**
     * 查询父级执行流程ID
     *
     * @param executionId 执行流程ID
     * @return 父级ID
     */
    String selectParentExecutionId(String executionId);
}
