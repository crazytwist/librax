package com.librax.lab.module.flow.dal;

import lombok.Data;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Data
public class FlowContext {

    /** 流程实例ID */
    private String processInstanceId;

    /** 当前逻辑节点ID（数据库里的） */
    private String currentNodeId;

    /** 上一个节点 */
    private String previousNodeId;

    /** 路由计算结果 */
    private String nextNodeId;

    /** 节点配置队列（给 dynamicNode 用） */
    private Deque<NodeConfig> nodeQueue = new ArrayDeque<>();

    /** 流程变量（给表达式 / 路由用） */
    private Map<String, Object> variables = new HashMap<>();

    /** 执行状态 */
    private String status;
}

