package com.librax.lab.module.task.stub;


/**
 * AGV 调度端客户端占位接口
 * TODO: 后续对接已有的 Java AGV 调度端
 *       对接方式：HTTP 调用 / RPC / MQ，由调度端实际通信协议决定
 */
public interface AgvDispatchClient {

    /**
     * 提交搬运任务
     *
     * @param job 搬运任务描述
     * @return jobId 调度端返回的任务ID，写入 lab_task.external_task_id
     */
    String submitJob(AgvJob job);

    /**
     * 取消搬运任务
     *
     * @param jobId 调度端任务ID
     */
    void cancelJob(String jobId);
}