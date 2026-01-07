package com.librax.lab.module.flow.service.impl;

import com.librax.lab.module.flow.dal.NodeConfig;
import com.librax.lab.module.flow.dal.NodeType;
import com.librax.lab.module.flow.dal.RouteRule;
import com.librax.lab.module.flow.service.NodeService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NodeServiceImpl implements NodeService {


    private final Map<String, NodeConfig> nodeConfigMap = new HashMap<>();
    private final List<RouteRule> routeRules = new ArrayList<>();

    private final ExpressionParser parser = new SpelExpressionParser();

    @PostConstruct
    public void init() {
        // 初始化节点配置
        nodeConfigMap.put("N1", new NodeConfig("N1", NodeType.VIRTUAL, Map.of(), false));
        nodeConfigMap.put("N2", new NodeConfig("N2", NodeType.AUTO, Map.of("taskName", "计算温度"), false));
        nodeConfigMap.put("N3", new NodeConfig("N3", NodeType.DISPATCH, Map.of("deviceType", "AGV"), false));
        nodeConfigMap.put("N4", new NodeConfig("N4", NodeType.MANUAL, Map.of("taskName", "人工确认"), false));
        nodeConfigMap.put("N5", new NodeConfig("N5", NodeType.END, Map.of(), true));

        // 初始化路由规则
        routeRules.add(new RouteRule("N1", "status == 'OK'", "N2"));
        routeRules.add(new RouteRule("N1", "status != 'OK'", "N4"));
        routeRules.add(new RouteRule("N2", "true", "N3"));
        routeRules.add(new RouteRule("N3", "true", "N5"));
        routeRules.add(new RouteRule("N4", "true", "N5"));
    }

    @Override
    public List<NodeConfig> loadNextNodes(String currentNodeId, Map<String, Object> variables) {
        List<NodeConfig> result = new ArrayList<>();

        System.out.println("currentNodeId: " + currentNodeId);
        System.out.println("variables: " + variables);

        // 找出所有 fromNodeId == currentNodeId 的路由
        List<RouteRule> rules = routeRules.stream()
                .filter(r -> r.getFromNodeId().equals(currentNodeId))
                .toList();

        rules.forEach(r -> System.out.println(
                "rule: " + r.getFromNodeId() + " -> " + r.getToNodeId() + ", condition: " + r.getCondition()
        ));

        for (RouteRule rule : rules) {
            boolean matched = evaluateCondition(rule.getCondition(), variables);
            System.out.println("Evaluating " + rule.getCondition() + " -> " + matched);

            if (matched) {
                NodeConfig nextNode = nodeConfigMap.get(rule.getToNodeId());
                if (nextNode != null) {
                    result.add(nextNode);
                    System.out.println("nextNode added: " + nextNode.getNodeId());
                } else {
                    System.out.println("NodeConfig not found for: " + rule.getToNodeId());
                }
            }
        }

        return result;
    }

    /** 使用 SpEL 判断条件是否成立 */
    private boolean evaluateCondition(String condition, Map<String, Object> variables) {
        if (condition == null || condition.isBlank()) {
            // 条件为空，默认匹配
            return true;
        }

        try {
            // 标准 EvaluationContext
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 兼容各种 Map 类型：都放到 #vars 中
            Map<String, Object> varsMap = new HashMap<>(variables);
            context.setVariable("vars", varsMap);

            // 将条件改写成使用 #vars['key'] 的方式，保证兼容 Hutool Map 等
            String expr = condition;
            if (!condition.contains("#vars")) {
                expr = condition.replaceAll("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b", "#vars['$1']");
            }

            Boolean value = parser.parseExpression(expr).getValue(context, Boolean.class);
            return Boolean.TRUE.equals(value);

        } catch (Exception e) {
            System.err.println("SpEL evaluate error: " + condition + ", variables: " + variables);
            e.printStackTrace();
            return false;
        }
    }



}
