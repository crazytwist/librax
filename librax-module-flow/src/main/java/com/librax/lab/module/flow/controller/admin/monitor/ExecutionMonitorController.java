package com.librax.lab.module.flow.controller.admin.monitor;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.flow.controller.admin.monitor.vo.ExecutionOverviewRespVO;
import com.librax.lab.module.flow.controller.admin.monitor.vo.ExecutionTimelineRespVO;
import com.librax.lab.module.flow.controller.admin.monitor.vo.RunningExecutionRespVO;
import com.librax.lab.module.flow.controller.admin.monitor.vo.StepStatusSummaryRespVO;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.executioneventlog.ExecutionEventLogMapper;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

/**
 * 执行监控接口
 * <p>
 * 提供运行中流程列表、执行概览、步骤状态汇总、事件时间线等监控能力。
 */
@Tag(name = "管理后台 - 执行监控")
@RestController
@RequestMapping("/flow/monitor")
@Validated
public class ExecutionMonitorController {

    @Resource
    private PipelineExecutionMapper executionMapper;
    @Resource
    private StepExecutionMapper stepMapper;
    @Resource
    private ExecutionEventLogMapper eventLogMapper;

    @GetMapping("/running")
    @Operation(summary = "运行中的流程列表")
    public CommonResult<List<RunningExecutionRespVO>> getRunningExecutions() {
        List<PipelineExecutionDO> running = executionMapper.selectByStatus(
                ExecutionStatusEnum.RUNNING.name());

        List<RunningExecutionRespVO> result = running.stream().map(exec -> {
            // 查该流程的步骤状态分布
            List<StepExecutionDO> steps = stepMapper.selectLatestByExecutionId(
                    exec.getExecutionId());
            Map<String, Long> statusCount = steps.stream()
                    .collect(Collectors.groupingBy(
                            StepExecutionDO::getStatus, Collectors.counting()));

            long elapsedMs = exec.getStartedAt() != null
                    ? Duration.between(exec.getStartedAt(), LocalDateTime.now()).toMillis()
                    : 0L;

            return RunningExecutionRespVO.builder()
                    .executionId(exec.getExecutionId())
                    .pipelineKey(exec.getPipelineKey())
                    .pipelineVersion(exec.getPipelineVersion())
                    .triggerType(exec.getTriggerType())
                    .triggeredBy(exec.getTriggeredBy())
                    .startedAt(exec.getStartedAt())
                    .elapsedMs(elapsedMs)
                    .totalSteps(steps.size())
                    .stepStatusCount(statusCount)
                    .build();
        }).collect(Collectors.toList());

        return success(result);
    }

    @GetMapping("/overview")
    @Operation(summary = "执行概览（各状态计数）")
    public CommonResult<ExecutionOverviewRespVO> getOverview() {
        ExecutionOverviewRespVO vo = new ExecutionOverviewRespVO();
        vo.setRunningCount(countByStatus(ExecutionStatusEnum.RUNNING.name()));
        vo.setPendingCount(countByStatus(ExecutionStatusEnum.PENDING.name()));
        vo.setPausedCount(countByStatus(ExecutionStatusEnum.PAUSED.name()));
        vo.setSuccessCount(countByStatus(ExecutionStatusEnum.SUCCESS.name()));
        vo.setFailedCount(countByStatus(ExecutionStatusEnum.FAILED.name()));
        return success(vo);
    }

    @GetMapping("/steps")
    @Operation(summary = "指定流程的步骤状态汇总")
    @Parameter(name = "executionId", description = "执行实例ID", required = true)
    public CommonResult<List<StepStatusSummaryRespVO>> getStepSummary(
            @RequestParam("executionId") String executionId) {
        List<StepExecutionDO> steps = stepMapper.selectLatestByExecutionId(executionId);

        List<StepStatusSummaryRespVO> result = steps.stream().map(step -> {
            long executeMs = step.getExecuteMs() != null ? step.getExecuteMs() : 0L;
            return StepStatusSummaryRespVO.builder()
                    .nodeId(step.getNodeId())
                    .stepKey(step.getStepKey())
                    .stepType(step.getStepType())
                    .status(step.getStatus())
                    .attempt(step.getAttempt())
                    .startedAt(step.getStartedAt())
                    .finishedAt(step.getFinishedAt())
                    .executeMs(executeMs)
                    .errorCode(step.getErrorCode())
                    .errorMsg(step.getErrorMsg())
                    .build();
        }).collect(Collectors.toList());

        return success(result);
    }

    @GetMapping("/timeline")
    @Operation(summary = "执行时间线（事件日志按时间排序）")
    @Parameter(name = "executionId", description = "执行实例ID", required = true)
    public CommonResult<List<ExecutionTimelineRespVO>> getTimeline(
            @RequestParam("executionId") String executionId) {
        List<ExecutionEventLogDO> events = eventLogMapper.selectByExecutionId(executionId);

        List<ExecutionTimelineRespVO> result = events.stream().map(e ->
                ExecutionTimelineRespVO.builder()
                        .eventType(e.getEventType())
                        .nodeId(e.getNodeId())
                        .attempt(e.getAttempt())
                        .fromStatus(e.getFromStatus())
                        .toStatus(e.getToStatus())
                        .payload(e.getPayload())
                        .operator(e.getOperator())
                        .occurredAt(e.getOccurredAt())
                        .build()
        ).collect(Collectors.toList());

        return success(result);
    }

    private long countByStatus(String status) {
        return executionMapper.selectCount(PipelineExecutionDO::getStatus, status);
    }
}
