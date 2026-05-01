package com.librax.lab.module.flow.engine.execution.recovery;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.librax.lab.module.flow.api.resource.ResourcePool;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.execution.scheduler.DagScheduler;
import com.librax.lab.module.flow.engine.execution.scheduler.StepSubmitter;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 断点恢复扫描器
 *
 * <p>应用启动后扫描所有"卡住"的流程，重新拉起调度。
 *
 * <p>处理策略：
 * <ul>
 *   <li>RUNNING 状态的步骤 → 重置为 PENDING（重新执行，要求执行器幂等）
 *   <li>WAITING 状态的步骤 → 保持不动（等外部回调）
 *   <li>流程重新触发 DagScheduler.schedule()
 * </ul>
 *
 * <p>使用 ApplicationReadyEvent 而非 @PostConstruct，
 * 确保所有 Bean 初始化完毕、数据库连接就绪后再执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecoveryScanner {

    private final PipelineExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final DagScheduler dagScheduler;
    private final ResourcePool resourcePool;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[RecoveryScanner] 应用启动，开始断点恢复扫描...");

        try {
            List<PipelineExecutionDO> runningExecutions = executionMapper
                    .selectByStatus(ExecutionStatusEnum.RUNNING.name());

            if (runningExecutions.isEmpty()) {
                log.info("[RecoveryScanner] 无需恢复的流程");
                return;
            }

            log.info("[RecoveryScanner] 发现 {} 个待恢复流程", runningExecutions.size());

            for (PipelineExecutionDO execution : runningExecutions) {
                try {
                    recoverExecution(execution);
                } catch (Exception e) {
                    log.error("[RecoveryScanner] 恢复失败 executionId={}",
                            execution.getExecutionId(), e);
                }
            }

            log.info("[RecoveryScanner] 断点恢复扫描完成");
        } catch (Exception e) {
            log.error("[RecoveryScanner] 扫描异常", e);
        }
    }

    /**
     * 恢复单个流程
     */
    private void recoverExecution(PipelineExecutionDO execution) {
        String executionId = execution.getExecutionId();

        // 1. 查出所有步骤的最新状态
        List<StepExecutionDO> steps = stepMapper.selectLatestByExecutionId(executionId);

        // 2. 找出 RUNNING 状态的步骤，重置为 PENDING
        List<StepExecutionDO> runningSteps = steps.stream()
                .filter(s -> StepStatusEnum.RUNNING.name().equals(s.getStatus()))
                .toList();

        int resetCount = 0;
        for (StepExecutionDO step : runningSteps) {
            int rows = resetToPending(step);
            if (rows > 0) {
                resetCount++;
                log.info("[RecoveryScanner] 步骤重置 RUNNING→PENDING executionId={} nodeId={} attempt={}",
                        executionId, step.getNodeId(), step.getAttempt());
            }
        }

        // 3. 统计 WAITING 状态的步骤（不动，只记录日志）
        long waitingCount = steps.stream()
                .filter(s -> StepStatusEnum.WAITING.name().equals(s.getStatus()))
                .count();

        log.info("[RecoveryScanner] 恢复流程 executionId={} pipelineKey={} " +
                        "totalSteps={} reset={} waiting={}",
                executionId, execution.getPipelineKey(),
                steps.size(), resetCount, waitingCount);

        // 4. 重新触发调度
        dagScheduler.schedule(
                executionId,
                execution.getPipelineKey(),
                execution.getPipelineVersion());
    }

    /**
     * 重置步骤状态 RUNNING → PENDING
     * 用 CAS 防止和其他线程冲突
     */
    private int resetToPending(StepExecutionDO step) {
        LocalDateTime now = LocalDateTime.now();
        return stepMapper.update(null, new LambdaUpdateWrapper<StepExecutionDO>()
                .eq(StepExecutionDO::getExecutionId, step.getExecutionId())
                .eq(StepExecutionDO::getNodeId, step.getNodeId())
                .eq(StepExecutionDO::getAttempt, step.getAttempt())
                .eq(StepExecutionDO::getStatus, StepStatusEnum.RUNNING.name())
                .set(StepExecutionDO::getStatus, StepStatusEnum.PENDING.name())
                .set(StepExecutionDO::getStartedAt, null)
                .set(StepExecutionDO::getUpdater, "RECOVERY")
                .set(StepExecutionDO::getUpdateTime, now));
    }

    private void recoverRunningStep(StepExecutionDO step) {
        // 查有没有未释放的 hold 记录
        String resourceId = resourcePool
                .queryHeldResourceId(step.getExecutionId(), step.getNodeId(), step.getAttempt());
        if (StringUtils.isNotBlank(resourceId)) {
            String holderKey = StepSubmitter.buildHolderKey(
                    step.getExecutionId(), step.getNodeId(), step.getAttempt());
            // 释放 Redis 锁 + 更新 hold 记录
            resourcePool.release(resourceId, holderKey);
//            resourceHoldMapper.markReleased(step.getExecutionId(), step.getNodeId(),
//                    step.getAttempt(), "RECOVERY", LocalDateTime.now());
            log.info("[RecoveryScanner] 释放宕机遗留资源 nodeId={} resourceId={}",
                    step.getNodeId(), resourceId);
        }
        // 原有重置逻辑
        stepMapper.resetToPending(step.getExecutionId(), step.getNodeId());
    }


}