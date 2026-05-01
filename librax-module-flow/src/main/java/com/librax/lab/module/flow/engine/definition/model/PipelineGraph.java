package com.librax.lab.module.flow.engine.definition.model;


import com.librax.lab.module.flow.enums.FailStrategyEnum;
import lombok.Data;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class PipelineGraph {

    // ---- 流程元信息 ----
    private String pipelineKey;
    private int version;
    private String name;
    private FailStrategyEnum failStrategy;
    private Long defaultTimeoutMs;


    // ---- 节点列表 ----
    private List<StepNode> steps;

    // ---- 快速查找 Map（nodeId → StepNode）----
    // Builder 填充，避免每次都遍历 List
    private Map<String, StepNode> stepIndex;

    // ---- JGraphT 有向无环图 ----
    // vertex = nodeId，edge = 依赖关系（从前置节点指向后续节点）
    // 只在 Validator 和关键路径分析时使用，调度器用 stepIndex 查依赖
    private DirectedAcyclicGraph<String, DefaultEdge> dag;

    /**
     * 通过 nodeId 查找节点，找不到抛异常
     */
    public StepNode getStep(String nodeId) {
        StepNode node = stepIndex.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("节点不存在: " + nodeId);
        }
        return node;
    }

    /**
     * 判断节点是否存在
     */
    public boolean containsNode(String nodeId) {
        return stepIndex.containsKey(nodeId);
    }

    /**
     * 获取入口节点（dependsOn 为空的节点）
     */
    public List<StepNode> getEntryNodes() {
        return steps.stream()
                .filter(s -> s.getDependsOn() == null || s.getDependsOn().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 初始化 stepIndex，由 Builder 在组装完 steps 后调用
     */
    public void buildIndex() {
        this.stepIndex = steps.stream()
                .collect(Collectors.toMap(StepNode::getNodeId, Function.identity()));
    }
}