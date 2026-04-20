package com.librax.lab.module.task.stub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AgvDispatchClientStub implements AgvDispatchClient {

    @Override
    public String submitJob(AgvJob job) {
        String jobId = "AGV-JOB-STUB-" + System.currentTimeMillis();
        log.info("[AgvDispatchClientStub] 提交AGV任务（STUB） jobId={} {}→{} token={}",
                jobId, job.getFrom(), job.getTo(), job.getCallbackToken());
        // TODO: 对接真实 AGV 调度端后删除此实现
        return jobId;
    }

    @Override
    public void cancelJob(String jobId) {
        log.info("[AgvDispatchClientStub] 取消AGV任务（STUB） jobId={}", jobId);
        // TODO: 对接真实 AGV 调度端后实现
    }
}