
package com.librax.lab.module.flow.engine.definition;

import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.enums.FailStrategyEnum;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PipelineGraphValidator 单元测试")
public class PipelineGraphValidatorTest {

    private PipelineGraphValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PipelineGraphValidator();
    }

    // ----------------------------------------------------------------
    // 正常场景：完整的水质检测流程应该通过
    // ----------------------------------------------------------------
    @Test
    @DisplayName("正常流程：水质检测7节点，应该校验通过")
    void test_validate_normalFlow_shouldPass() {
        PipelineGraph graph = buildWaterQualityGraph();
        // 不抛异常即为通过
        assertDoesNotThrow(() -> validator.validate(graph));
        // 校验通过后 dag 字段应该被填充
        assertNotNull(graph.getDag(), "校验通过后 dag 字段应该被填充");
        assertEquals(7, graph.getDag().vertexSet().size(), "DAG 应该有7个节点");
    }

    // ----------------------------------------------------------------
    // 规则1：没有入口节点
    // ----------------------------------------------------------------
    @Test
    @DisplayName("规则1：所有节点都有 dependsOn，应该报错")
    void test_validate_noEntryNode_shouldFail() {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        // 两个节点互相依赖（同时也违反了环检测，但规则1先触发）
        StepNode s1 = buildComputeNode("s1", List.of("s2"));
        StepNode s2 = buildComputeNode("s2", List.of("s1"));
        graph.setSteps(Arrays.asList(s1, s2));
        graph.buildIndex();

        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        assertTrue(ex.getMessage().contains("没有入口节点"),
                "错误信息应包含「没有入口节点」，实际: " + ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 规则2：dependsOn 引用了不存在的节点
    // ----------------------------------------------------------------
    @Test
    @DisplayName("规则2：dependsOn 引用不存在节点，应该报错")
    void test_validate_dependsOnNotExists_shouldFail() {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        StepNode s1 = buildComputeNode("s1", Collections.emptyList());
        // s2 依赖一个不存在的节点 s_ghost
        StepNode s2 = buildComputeNode("s2", List.of("s_ghost"));
        graph.setSteps(Arrays.asList(s1, s2));
        graph.buildIndex();

        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        assertTrue(ex.getMessage().contains("s_ghost"),
                "错误信息应包含不存在的节点ID「s_ghost」");
    }

    // ----------------------------------------------------------------
    // 规则3：CONDITION 节点缺少必要字段
    // ----------------------------------------------------------------
    @Test
    @DisplayName("规则3a：CONDITION 节点缺少 conditionExpr，应该报错")
    void test_validate_conditionMissingExpr_shouldFail() {
        PipelineGraph graph = buildGraphWithCondition(null, "s_pass", "s_fail");
        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        assertTrue(ex.getMessage().contains("condition_expr"),
                "错误信息应包含「condition_expr」");
    }

    @Test
    @DisplayName("规则3b：CONDITION 节点的 trueBranch 不存在，应该报错")
    void test_validate_conditionTrueBranchNotExists_shouldFail() {
        PipelineGraph graph = buildGraphWithCondition(
                "${s1.score} >= 80", "s_not_exist", "s_fail");
        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        assertTrue(ex.getMessage().contains("true_branch"),
                "错误信息应包含「true_branch」");
    }

    // ----------------------------------------------------------------
    // 规则4：分支节点的 dependsOn 没有包含 CONDITION 节点
    // ----------------------------------------------------------------
    @Test
    @DisplayName("规则4：分支节点 dependsOn 未包含 CONDITION 节点，应该报错")
    void test_validate_branchNotDependsOnCondition_shouldFail() {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        StepNode s1    = buildComputeNode("s1", Collections.emptyList());
        StepNode judge = buildConditionNode("s_judge", List.of("s1"),
                "${s1.score} >= 80", "s_pass", "s_fail");
        // s_pass 的 dependsOn 没有包含 s_judge（错误！）
        StepNode pass  = buildComputeNode("s_pass", List.of("s1"));
        StepNode fail  = buildComputeNode("s_fail", List.of("s_judge"));

        graph.setSteps(Arrays.asList(s1, judge, pass, fail));
        graph.buildIndex();

        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        assertTrue(ex.getMessage().contains("s_pass") || ex.getMessage().contains("true_branch"),
                "错误信息应涉及 s_pass 节点");
    }

    // ----------------------------------------------------------------
    // 规则5：INSTRUMENT 节点缺少 deviceType
    // ----------------------------------------------------------------
    @Test
    @DisplayName("规则5：INSTRUMENT 节点缺少 deviceType，应该报错")
    void test_validate_instrumentMissingDeviceType_shouldFail() {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        // deviceType 为空
        StepNode s1 = StepNode.builder()
                .nodeId("s1")
                .stepKey("ph_measure")
                .name("PH检测")
                .stepType(StepTypeEnum.INSTRUMENT)
                .dependsOn(Collections.emptyList())
                .deviceType(null)   // 缺少
                .command("MEASURE")
                .timeoutMs(15000L)
                .maxAttempts(3)
                .backoffMs(1000L)
                .onFailure(FailStrategyEnum.FAIL_FAST)
                .params(Map.of())
                .build();

        graph.setSteps(List.of(s1));
        graph.buildIndex();

        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        assertTrue(ex.getMessage().contains("device_type"),
                "错误信息应包含「device_type」");
    }

    // ----------------------------------------------------------------
    // 规则6：COMPUTE 节点 executor=BEAN 但缺少 beanName
    // ----------------------------------------------------------------
    @Test
    @DisplayName("规则6：COMPUTE executor=BEAN 缺少 beanName，应该报错")
    void test_validate_computeMissingBeanName_shouldFail() {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        StepNode s1 = StepNode.builder()
                .nodeId("s1")
                .stepKey("calc")
                .name("计算")
                .stepType(StepTypeEnum.COMPUTE)
                .dependsOn(Collections.emptyList())
                .executor("BEAN")
                .beanName(null)    // 缺少
                .methodName("execute")
                .timeoutMs(5000L)
                .maxAttempts(1)
                .backoffMs(0L)
                .onFailure(FailStrategyEnum.FAIL_FAST)
                .params(Map.of())
                .build();

        graph.setSteps(List.of(s1));
        graph.buildIndex();

        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        assertTrue(ex.getMessage().contains("bean_name"),
                "错误信息应包含「bean_name」");
    }

    // ----------------------------------------------------------------
    // 规则7：有环
    // ----------------------------------------------------------------
    @Test
    @DisplayName("规则7：流程存在循环依赖，应该报错")
    void test_validate_cycleDetected_shouldFail() {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        // s1 是入口，s2 → s3 → s2 形成环
        StepNode s1 = buildComputeNode("s1", Collections.emptyList());
        StepNode s2 = buildComputeNode("s2", List.of("s1", "s3")); // s2 依赖 s3
        StepNode s3 = buildComputeNode("s3", List.of("s2"));        // s3 依赖 s2（成环）

        graph.setSteps(Arrays.asList(s1, s2, s3));
        graph.buildIndex();

        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        assertTrue(ex.getMessage().contains("循环依赖"),
                "错误信息应包含「循环依赖」，实际: " + ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 错误收集：多个错误一次返回
    // ----------------------------------------------------------------
    @Test
    @DisplayName("多个规则同时违反，错误信息应包含所有错误")
    void test_validate_multipleErrors_shouldCollectAll() {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        // INSTRUMENT 缺 deviceType + COMPUTE 缺 beanName，同时有两个错误
        StepNode s1 = StepNode.builder()
                .nodeId("s1").stepKey("k1").name("仪器")
                .stepType(StepTypeEnum.INSTRUMENT)
                .dependsOn(Collections.emptyList())
                .deviceType(null).command(null)
                .timeoutMs(1000L).maxAttempts(1).backoffMs(0L)
                .onFailure(FailStrategyEnum.FAIL_FAST).params(Map.of()).build();

        StepNode s2 = StepNode.builder()
                .nodeId("s2").stepKey("k2").name("计算")
                .stepType(StepTypeEnum.COMPUTE)
                .dependsOn(List.of("s1"))
                .executor("BEAN").beanName(null).methodName(null)
                .timeoutMs(1000L).maxAttempts(1).backoffMs(0L)
                .onFailure(FailStrategyEnum.FAIL_FAST).params(Map.of()).build();

        graph.setSteps(Arrays.asList(s1, s2));
        graph.buildIndex();

        Exception ex = assertThrows(Exception.class, () -> validator.validate(graph));
        String msg = ex.getMessage();
        assertTrue(msg.contains("device_type"), "应包含 device_type 错误");
        assertTrue(msg.contains("bean_name"),   "应包含 bean_name 错误");
    }

    // ================================================================
    // 构建测试用 PipelineGraph 的辅助方法
    // ================================================================

    /** 构建完整的水质检测流程图（7节点，应该校验通过）*/
    private PipelineGraph buildWaterQualityGraph() {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("water_quality_test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        StepNode sample    = buildInstrumentNode("s_sample",    Collections.emptyList());
        StepNode ph        = buildInstrumentNode("s_ph",        List.of("s_sample"));
        StepNode turbidity = buildInstrumentNode("s_turbidity", List.of("s_sample"));
        StepNode calc      = buildComputeNode("s_calc",         List.of("s_ph", "s_turbidity"));
        StepNode judge     = buildConditionNode("s_judge",      List.of("s_calc"),
                "${s_calc.score} >= 80", "s_archive", "s_retest_notify");
        StepNode archive   = buildComputeNode("s_archive",      List.of("s_judge"));
        StepNode retest    = buildNotifyNode("s_retest_notify", List.of("s_judge"));

        graph.setSteps(Arrays.asList(sample, ph, turbidity, calc, judge, archive, retest));
        graph.buildIndex();
        return graph;
    }

    private StepNode buildInstrumentNode(String nodeId, List<String> dependsOn) {
        return StepNode.builder()
                .nodeId(nodeId).stepKey("instrument_" + nodeId).name(nodeId)
                .stepType(StepTypeEnum.INSTRUMENT)
                .dependsOn(dependsOn)
                .deviceType("PH_METER").command("MEASURE")
                .timeoutMs(15000L).maxAttempts(3).backoffMs(1000L)
                .onFailure(FailStrategyEnum.FAIL_FAST).params(Map.of()).build();
    }

    private StepNode buildComputeNode(String nodeId, List<String> dependsOn) {
        return StepNode.builder()
                .nodeId(nodeId).stepKey("compute_" + nodeId).name(nodeId)
                .stepType(StepTypeEnum.COMPUTE)
                .dependsOn(dependsOn)
                .executor("BEAN").beanName("testBean").methodName("execute")
                .timeoutMs(5000L).maxAttempts(1).backoffMs(0L)
                .onFailure(FailStrategyEnum.FAIL_FAST).params(Map.of()).build();
    }

    private StepNode buildConditionNode(String nodeId, List<String> dependsOn,
                                        String expr, String trueBranch, String falseBranch) {
        return StepNode.builder()
                .nodeId(nodeId).stepKey("condition_" + nodeId).name(nodeId)
                .stepType(StepTypeEnum.CONDITION)
                .dependsOn(dependsOn)
                .conditionExpr(expr).trueBranch(trueBranch).falseBranch(falseBranch)
                .timeoutMs(1000L).maxAttempts(1).backoffMs(0L)
                .onFailure(FailStrategyEnum.FAIL_FAST).params(Map.of()).build();
    }

    private StepNode buildNotifyNode(String nodeId, List<String> dependsOn) {
        return StepNode.builder()
                .nodeId(nodeId).stepKey("notify_" + nodeId).name(nodeId)
                .stepType(StepTypeEnum.NOTIFY)
                .dependsOn(dependsOn)
                .timeoutMs(5000L).maxAttempts(2).backoffMs(2000L)
                .onFailure(FailStrategyEnum.SKIP).params(Map.of()).build();
    }

    /** 构建一个含 CONDITION 节点的简单图（用于规则3/4的测试）*/
    private PipelineGraph buildGraphWithCondition(String expr,
                                                  String trueBranch,
                                                  String falseBranch) {
        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey("test");
        graph.setVersion(1);
        graph.setFailStrategy(FailStrategyEnum.FAIL_FAST);

        StepNode s1    = buildComputeNode("s1", Collections.emptyList());
        StepNode judge = buildConditionNode("s_judge", List.of("s1"),
                expr, trueBranch, falseBranch);
        StepNode pass  = buildComputeNode("s_pass",   List.of("s_judge"));
        StepNode fail  = buildComputeNode("s_fail",   List.of("s_judge"));

        graph.setSteps(Arrays.asList(s1, judge, pass, fail));
        graph.buildIndex();
        return graph;
    }
}