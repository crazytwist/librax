package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.execution.statemachine.StepStateMachine;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分支跳过处理器 — 负责递归标记未选中分支为 SKIPPED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BranchSkipper {

    private final StepExecutionMapper stepMapper;
    private final StepStateMachine stepStateMachine;

    /**
     * 递归标记未选中分支及其所有下游节点为 SKIPPED
     *
     * 入口方法：一次性加载状态快照，递归时从 Map 读，不再查 DB
     */
    public void markBranchSkipped(String executionId,
                                  PipelineGraph graph,
                                  String startNodeId,
                                  String conditionNodeId) {
        // 一次性加载整个执行实例的步骤状态（1次DB查询）
        Map<String, StepExecutionDO> snapshot = stepMapper
                .selectLatestByExecutionId(executionId)
                .stream()
                .collect(Collectors.toMap(
                        StepExecutionDO::getNodeId,
                        s -> s,
                        (a, b) -> b));

        doMarkSkipped(executionId, graph, startNodeId, conditionNodeId, snapshot);
    }

    /**
     * 递归实现，状态全部从 snapshot 读取
     *
     * @param snapshot key=nodeId，递归过程中实时更新，保证后续判断准确
     */
    private void doMarkSkipped(String executionId,
                               PipelineGraph graph,
                               String nodeId,
                               String conditionNodeId,
                               Map<String, StepExecutionDO> snapshot) {
        if (nodeId == null || !graph.containsNode(nodeId)) return;

        StepExecutionDO stepDO = snapshot.get(nodeId);
        if (stepDO == null) return;

        StepStatusEnum current = StepStatusEnum.valueOf(stepDO.getStatus());
        if (current.isTerminal()) return;

        // 标记当前节点 SKIPPED
        stepStateMachine.markSkipped(executionId, nodeId, stepDO.getAttempt());

        // 同步更新快照，让后续依赖判断能看到最新状态
        stepDO.setStatus(StepStatusEnum.SKIPPED.name());

        // 递归处理：找出以当前节点为依赖的下游节点
        graph.getSteps().stream()
                .filter(n -> n.getDependsOn() != null
                        && n.getDependsOn().contains(nodeId)
                        && !n.getNodeId().equals(conditionNodeId))
                .forEach(n -> {
                    boolean allDepsSkippedOrSuccess = n.getDependsOn().stream()
                            .allMatch(dep -> {
                                StepExecutionDO depDO = snapshot.get(dep);
                                if (depDO == null) return false;
                                StepStatusEnum s = StepStatusEnum.valueOf(depDO.getStatus());
                                return s == StepStatusEnum.SKIPPED
                                        || s == StepStatusEnum.SUCCESS;
                            });
                    if (allDepsSkippedOrSuccess) {
                        doMarkSkipped(executionId, graph,
                                n.getNodeId(), conditionNodeId, snapshot);
                    }
                });
    }
}