package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.api.resource.ResourceReleasedEvent;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 资源释放唤醒监听器
 *
 * <p>监听 {@link ResourceReleasedEvent}，从 {@link ResourceWaitRegistry} 取出
 * 所有在等待该资源类型的 executionId，对仍处于 RUNNING 的流程重新触发 DAG 调度。
 *
 * <p>调度触发后，{@link DagScheduler#doSchedule} 会扫描所有 PENDING 步骤，
 * 满足依赖的步骤将再次调用 {@code acquire()}；此时资源已释放，申请可成功，步骤继续执行。
 *
 * <p>使用 {@code @Async} 避免阻塞 release 的调用线程（通常是步骤完成回调线程）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceReleaseWakeupListener {

    private final ResourceWaitRegistry    waitRegistry;
    private final DagScheduler            dagScheduler;
    private final PipelineExecutionMapper executionMapper;

    @Async
    @EventListener
    public void onResourceReleased(ResourceReleasedEvent event) {
        String resourceType = event.getResourceType();
        String zoneCode     = event.getZoneCode();

        // 取出并清空等待该资源类型的所有 executionId
        Set<String> waitingIds = waitRegistry.popAll(resourceType, zoneCode);
        if (waitingIds.isEmpty()) {
            return;
        }

        log.info("[ResourceReleaseWakeup] 资源释放，唤醒等待流程 type={} zone={} count={}",
                resourceType, zoneCode, waitingIds.size());

        for (String executionId : waitingIds) {
            try {
                PipelineExecutionDO execution =
                        executionMapper.selectByExecutionId(executionId);

                // 只唤醒仍在 RUNNING 的流程
                if (execution == null
                        || !ExecutionStatusEnum.RUNNING.name().equals(execution.getStatus())) {
                    log.debug("[ResourceReleaseWakeup] 跳过已结束的流程 executionId={}", executionId);
                    continue;
                }

                log.info("[ResourceReleaseWakeup] 唤醒调度 executionId={} pipeline={}:{}",
                        executionId,
                        execution.getPipelineKey(),
                        execution.getPipelineVersion());

                dagScheduler.schedule(
                        executionId,
                        execution.getPipelineKey(),
                        execution.getPipelineVersion());

            } catch (Exception e) {
                log.error("[ResourceReleaseWakeup] 唤醒异常 executionId={}", executionId, e);
            }
        }
    }
}
