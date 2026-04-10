package com.librax.lab.module.flow.api;


import java.util.Map;

/**
 * 流程启动前置钩子 SPI
 *
 * flow 模块在 start() 事务内、调度器触发前调用所有注册的钩子。
 * lab 模块实现此接口，完成样本预绑定等同步操作。
 *
 * 设计约束：
 *   - 实现类必须是幂等的（断点恢复时可能重复调用）
 *   - 抛出异常会导致整个 start() 事务回滚
 *   - 执行顺序由 @Order 控制
 */
public interface PipelineStartHook {
    /**
     * @param executionId  本次执行ID
     * @param pipelineKey  流程标识
     * @param version      流程版本
     * @param inputParams  流程初始参数
     */
    void beforeSchedule(String executionId,
                        String pipelineKey,
                        int version,
                        Map<String, Object> inputParams);
}
