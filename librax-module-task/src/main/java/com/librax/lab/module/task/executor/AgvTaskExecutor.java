package com.librax.lab.module.task.executor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dal.mysql.task.TaskMapper;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import com.librax.lab.module.task.stub.AgvDispatchClient;
import com.librax.lab.module.task.stub.AgvJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgvTaskExecutor implements TaskExecutor {

    private final AgvDispatchClient agvDispatchClient;
    private final TaskMapper taskMapper;

    @Override
    public String supportType() {
        return TaskTypeEnum.AGV.name();
    }

    @Override
    public void execute(TaskDO task) {
        Map<String, Object> payload = parsePayload(task);
        String fromStation = (String) payload.get("fromStation");
        String toStation   = (String) payload.get("toStation");
        String materialId  = (String) payload.get("materialId");

        // TODO: 资源模块接入后，AGV 选择逻辑移到 ResourcePool
        //       目前由 AgvDispatchClient 内部选车
        String jobId = agvDispatchClient.submitJob(AgvJob.builder()
                .from(fromStation)
                .to(toStation)
                .materialId(materialId)
                .priority(task.getPriority())
                .callbackToken(task.getCallbackToken())
                .build());

        taskMapper.updateExecuting(task.getTaskId(), jobId);

        log.info("[AgvTaskExecutor] AGV任务已提交 taskId={} jobId={} {}→{}",
                task.getTaskId(), jobId, fromStation, toStation);
    }

    @Override
    public void cancel(TaskDO task) {
        if (task.getExternalTaskId() != null) {
            try {
                agvDispatchClient.cancelJob(task.getExternalTaskId());
            } catch (Exception e) {
                log.warn("[AgvTaskExecutor] 取消AGV任务失败（忽略） taskId={} error={}",
                        task.getTaskId(), e.getMessage());
            }
        }
    }

    private Map<String, Object> parsePayload(TaskDO task) {
        return JSON.parseObject(task.getPayload(),
                new TypeReference<Map<String, Object>>() {});
    }
}