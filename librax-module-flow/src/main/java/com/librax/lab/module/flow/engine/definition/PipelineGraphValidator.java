package com.librax.lab.module.flow.engine.definition;

import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.DirectedPseudograph;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 流程图校验器
 *
 * <p>输入：已由 {@link PipelineGraphBuilder} 组装完成的 {@link PipelineGraph}
 * <p>输出：校验通过则填充 dag 字段；失败则抛出包含所有错误的异常（一次返回全部问题）
 *
 * <p>校验顺序：
 * <ol>
 *   <li>结构校验（6条规则）：收集所有错误，一次性抛出
 *   <li>环检测（JGraphT）：结构正确才有意义
 *   <li>构建 DAG：校验全部通过后写入 graph.dag
 * </ol>
 */
@Slf4j
@Component
public class PipelineGraphValidator {

    /**
     * 校验入口
     *
     * @param graph 待校验的流程图
     * @throws com.librax.lab.framework.common.exception.ServiceException 校验失败时抛出
     */
    public void validate(PipelineGraph graph) {
        List<String> errors = new ArrayList<>();

        // ── 结构校验（先收集所有错误，方便一次性看到全部问题）──────────
        checkEntryNodes(graph, errors);
        checkDependsOnExists(graph, errors);
        checkConditionNode(graph, errors);
        checkBranchDependsOnCondition(graph, errors);
        checkInstrumentDeviceType(graph, errors);
        checkComputeExecutor(graph, errors);

        // 结构有问题时停止，环检测在破损的图上结果不可信
        if (!errors.isEmpty()) {
            String msg = buildErrorMessage(graph, errors);
            log.warn("[PipelineGraphValidator] 校验失败\n{}", msg);
            throw exception(PIPELINE_GRAPH_VALIDATE_FAIL, msg);
        }

        // ── 环检测 ────────────────────────────────────────────────────
        checkNoCycle(graph, errors);
        if (!errors.isEmpty()) {
            String msg = buildErrorMessage(graph, errors);
            log.warn("[PipelineGraphValidator] 环检测失败\n{}", msg);
            throw exception(PIPELINE_GRAPH_VALIDATE_FAIL, msg);
        }

        // ── 全部通过，构建 JGraphT DAG 写入 graph ─────────────────────
        graph.setDag(buildDag(graph));
        log.info("[PipelineGraphValidator] 校验通过 pipeline_key={} version={}",
                graph.getPipelineKey(), graph.getVersion());
    }

    // ================================================================
    // 规则1：必须有至少一个入口节点（dependsOn 为空）
    // ================================================================
    private void checkEntryNodes(PipelineGraph graph, List<String> errors) {
        boolean hasEntry = graph.getSteps().stream()
                .anyMatch(s -> s.getDependsOn() == null || s.getDependsOn().isEmpty());
        if (!hasEntry) {
            errors.add("没有入口节点：所有节点都有 dependsOn，流程无法启动");
        }
    }

    // ================================================================
    // 规则2：dependsOn 引用的 nodeId 必须在本流程内存在
    // ================================================================
    private void checkDependsOnExists(PipelineGraph graph, List<String> errors) {
        for (StepNode node : graph.getSteps()) {
            if (node.getDependsOn() == null) {
                continue;
            }
            for (String dep : node.getDependsOn()) {
                if (!graph.containsNode(dep)) {
                    errors.add(String.format(
                            "节点 [%s] 的 dependsOn 引用了不存在的节点 [%s]",
                            node.getNodeId(), dep));
                }
            }
        }
    }

    // ================================================================
    // 规则3：CONDITION 节点的完整性校验
    //   3a. 必须有 conditionExpr
    //   3b. trueBranch / falseBranch 不能为空
    //   3c. trueBranch / falseBranch 引用的节点必须存在
    // ================================================================
    private void checkConditionNode(PipelineGraph graph, List<String> errors) {
        graph.getSteps().stream()
                .filter(s -> s.getStepType() == StepTypeEnum.CONDITION)
                .forEach(node -> {
                    String nid = node.getNodeId();

                    // 3a. 表达式不能为空
                    if (!StringUtils.hasText(node.getConditionExpr())) {
                        errors.add(String.format(
                                "CONDITION 节点 [%s] 缺少 condition_expr", nid));
                    }

                    // 3b & 3c. true 分支
                    if (!StringUtils.hasText(node.getTrueBranch())) {
                        errors.add(String.format(
                                "CONDITION 节点 [%s] 缺少 true_branch", nid));
                    } else if (!graph.containsNode(node.getTrueBranch())) {
                        errors.add(String.format(
                                "CONDITION 节点 [%s] 的 true_branch [%s] 不存在",
                                nid, node.getTrueBranch()));
                    }

                    // 3b & 3c. false 分支
                    if (!StringUtils.hasText(node.getFalseBranch())) {
                        errors.add(String.format(
                                "CONDITION 节点 [%s] 缺少 false_branch", nid));
                    } else if (!graph.containsNode(node.getFalseBranch())) {
                        errors.add(String.format(
                                "CONDITION 节点 [%s] 的 false_branch [%s] 不存在",
                                nid, node.getFalseBranch()));
                    }
                });
    }

    // ================================================================
    // 规则4：CONDITION 的分支节点必须将该 CONDITION 节点列在自己的 dependsOn 中
    //   保证调度器能感知到分支依赖关系，SKIPPED 状态才能正确传播
    // ================================================================
    private void checkBranchDependsOnCondition(PipelineGraph graph, List<String> errors) {
        graph.getSteps().stream()
                .filter(s -> s.getStepType() == StepTypeEnum.CONDITION)
                .forEach(condition -> {
                    checkSingleBranch(graph, condition.getNodeId(),
                            condition.getTrueBranch(), "true_branch", errors);
                    checkSingleBranch(graph, condition.getNodeId(),
                            condition.getFalseBranch(), "false_branch", errors);
                });
    }

    private void checkSingleBranch(PipelineGraph graph, String conditionId,
                                   String branchNodeId, String label,
                                   List<String> errors) {
        // 节点不存在时已在规则3中报错，这里跳过避免重复
        if (!StringUtils.hasText(branchNodeId) || !graph.containsNode(branchNodeId)) {
            return;
        }

        StepNode branch = graph.getStep(branchNodeId);
        boolean hasDep = branch.getDependsOn() != null
                && branch.getDependsOn().contains(conditionId);
        if (!hasDep) {
            errors.add(String.format(
                    "CONDITION 节点 [%s] 的 %s [%s] 的 dependsOn 中未包含该 CONDITION 节点",
                    conditionId, label, branchNodeId));
        }
    }

    // ================================================================
    // 规则5：INSTRUMENT 节点必须有 deviceType
    // ================================================================
    private void checkInstrumentDeviceType(PipelineGraph graph, List<String> errors) {
        graph.getSteps().stream()
                .filter(s -> s.getStepType() == StepTypeEnum.INSTRUMENT)
                .forEach(node -> {
                    if (!StringUtils.hasText(node.getDeviceType())) {
                        errors.add(String.format(
                                "INSTRUMENT 节点 [%s] 缺少 device_type", node.getNodeId()));
                    }
                    if (!StringUtils.hasText(node.getCommand())) {
                        errors.add(String.format(
                                "INSTRUMENT 节点 [%s] 缺少 command", node.getNodeId()));
                    }
                });
    }

    // ================================================================
    // 规则6：COMPUTE 节点的 executor 及其对应配置必须完整
    // ================================================================
    private void checkComputeExecutor(PipelineGraph graph, List<String> errors) {
        graph.getSteps().stream()
                .filter(s -> s.getStepType() == StepTypeEnum.COMPUTE)
                .forEach(node -> {
                    String nid = node.getNodeId();
                    if (!StringUtils.hasText(node.getExecutor())) {
                        errors.add(String.format(
                                "COMPUTE 节点 [%s] 缺少 executor", nid));
                        return; // executor 都没有，后续校验无意义
                    }
                    switch (node.getExecutor()) {
                        case "BEAN" -> {
                            if (!StringUtils.hasText(node.getBeanName())) {
                                errors.add(String.format(
                                        "COMPUTE 节点 [%s] executor=BEAN 时 bean_name 不能为空", nid));
                            }
                            if (!StringUtils.hasText(node.getMethodName())) {
                                errors.add(String.format(
                                        "COMPUTE 节点 [%s] executor=BEAN 时 method_name 不能为空", nid));
                            }
                        }
                        case "LITEFLOW" -> {
                            if (!StringUtils.hasText(node.getChainId())) {
                                errors.add(String.format(
                                        "COMPUTE 节点 [%s] executor=LITEFLOW 时 chain_id 不能为空", nid));
                            }
                        }
                        default -> errors.add(String.format(
                                "COMPUTE 节点 [%s] 的 executor [%s] 不支持，仅支持 BEAN / LITEFLOW",
                                nid, node.getExecutor()));
                    }
                });
    }

    // ================================================================
    // 环检测：用 DirectedPseudograph 建图（比 DAG 宽松，有环不抛异常只报告）
    // ================================================================
    private void checkNoCycle(PipelineGraph graph, List<String> errors) {
        DirectedPseudograph<String, DefaultEdge> pseudoGraph =
                new DirectedPseudograph<>(DefaultEdge.class);

        // 添加所有节点
        graph.getSteps().forEach(s -> pseudoGraph.addVertex(s.getNodeId()));

        // 添加依赖边：前置节点 → 当前节点
        for (StepNode node : graph.getSteps()) {
            if (node.getDependsOn() == null) {
                continue;
            }
            for (String dep : node.getDependsOn()) {
                if (pseudoGraph.containsVertex(dep)) {
                    pseudoGraph.addEdge(dep, node.getNodeId());
                }
            }
        }

        CycleDetector<String, DefaultEdge> detector = new CycleDetector<>(pseudoGraph);
        if (detector.detectCycles()) {
            Set<String> cycleNodes = detector.findCycles();
            errors.add("存在循环依赖，涉及节点: " + cycleNodes);
        }
    }

    // ================================================================
    // 构建 JGraphT DirectedAcyclicGraph（校验全通过后调用）
    // ================================================================
    private DirectedAcyclicGraph<String, DefaultEdge> buildDag(PipelineGraph graph) {
        DirectedAcyclicGraph<String, DefaultEdge> dag =
                new DirectedAcyclicGraph<>(DefaultEdge.class);

        graph.getSteps().forEach(s -> dag.addVertex(s.getNodeId()));
        for (StepNode node : graph.getSteps()) {
            if (node.getDependsOn() == null) {
                continue;
            }
            for (String dep : node.getDependsOn()) {
                dag.addEdge(dep, node.getNodeId());
            }
        }
        return dag;
    }

    // ================================================================
    // 格式化错误信息
    // ================================================================
    private String buildErrorMessage(PipelineGraph graph, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s v%d] 共 %d 个错误：%n",
                graph.getPipelineKey(), graph.getVersion(), errors.size()));
        for (int i = 0; i < errors.size(); i++) {
            sb.append(String.format("  %d. %s%n", i + 1, errors.get(i)));
        }
        return sb.toString();
    }
}