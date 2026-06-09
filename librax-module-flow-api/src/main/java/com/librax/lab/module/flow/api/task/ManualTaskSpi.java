package com.librax.lab.module.flow.api.task;

/**
 * 人工任务 SPI
 * <p>
 * 定义在 flow-api 模块，由 task 模块实现，供 flow 模块的 ManualStepExecutor 调用。
 * 遵循与 {@link com.librax.lab.module.flow.api.material.MaterialCheckSpi} 相同的 SPI 模式，
 * 避免 flow ↔ task 之间的循环依赖。
 */
public interface ManualTaskSpi {

    /**
     * 创建人工任务并推入执行队列
     *
     * @param request 任务创建请求
     * @return taskId  lab_task.task_id，写入步骤输出供追踪
     */
    String submitTask(ManualTaskRequest request);
}
