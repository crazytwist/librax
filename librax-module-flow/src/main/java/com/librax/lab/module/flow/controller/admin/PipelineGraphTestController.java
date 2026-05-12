package com.librax.lab.module.flow.controller.admin;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.flow.controller.app.vo.PipelineStartReqVO;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.service.pipelineexecution.PipelineExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 临时验证接口，验证完删掉
 */
@Tag(name = "临时验证接口，验证完删掉")
@RestController
@RequestMapping("/flow/test")
@RequiredArgsConstructor
public class PipelineGraphTestController {

    private final PipelineGraphCache graphCache;
    private final PipelineExecutionService executionService;
    private final PipelineExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;

    /**
     * 验证流程图加载是否正确
     * GET /admin-api/flow/test/graph?pipelineKey=water_quality_test&version=1
     */
    @Operation(summary = "验证流程图加载是否正确")
    @GetMapping("/graph")
    public CommonResult<Map<String, Object>> getGraph(
            @RequestParam String pipelineKey,
            @RequestParam Integer version) {

        PipelineGraph graph = graphCache.get(pipelineKey, version);
        // 组装返回信息，方便肉眼核对
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pipelineKey",  graph.getPipelineKey());
        result.put("version",      graph.getVersion());
        result.put("name",         graph.getName());
        result.put("failStrategy", graph.getFailStrategy());
        result.put("totalNodes",   graph.getSteps().size());
        result.put("dagVertices",  graph.getDag().vertexSet().size());

        // 入口节点
        List<String> entryNodes = graph.getEntryNodes().stream()
                .map(StepNode::getNodeId)
                .collect(Collectors.toList());
        result.put("entryNodes", entryNodes);

        // 每个节点的关键信息
        List<Map<String, Object>> nodes = graph.getSteps().stream().map(node -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("nodeId",       node.getNodeId());
            n.put("stepType",     node.getStepType());
            n.put("dependsOn",    node.getDependsOn());
            n.put("params",       node.getParams());          // 验证三层参数合并
            n.put("timeoutMs",    node.getTimeoutMs());       // 验证超时合并
            n.put("maxAttempts",  node.getMaxAttempts());
            // CONDITION 节点额外显示分支信息
            if (node.getConditionExpr() != null) {
                n.put("conditionExpr", node.getConditionExpr());
                n.put("trueBranch",    node.getTrueBranch());
                n.put("falseBranch",   node.getFalseBranch());
            }
            // INSTRUMENT 节点显示设备信息
            if (node.getDeviceType() != null) {
                n.put("deviceType", node.getDeviceType());
                n.put("command",    node.getCommand());
            }
            return n;
        }).collect(Collectors.toList());
        result.put("nodes", nodes);

        return CommonResult.success(result);
    }

    /**
     * 验证缓存失效是否正常
     * DELETE /admin-api/flow/test/graph/cache?pipelineKey=water_quality_test&version=1
     */
    @Operation(summary = "验证缓存失效是否正常")
    @DeleteMapping("/graph/cache")
    public CommonResult<String> invalidateCache(
            @RequestParam String pipelineKey,
            @RequestParam Integer version) {
        graphCache.invalidate(pipelineKey, version);
        return CommonResult.success("缓存已失效，再次调用 GET 接口会重新加载");
    }


    /**
     * 启动一条流程（Mock模式）
     * POST /admin-api/flow/test/start
     * Body: {"pipelineKey":"water_quality_test","version":1,"sampleId":"S-001"}
     */
    @Operation(summary = "启动一条流程")
    @PostMapping("/start")
    public CommonResult<Map<String, Object>> start(@RequestBody @Valid PipelineStartReqVO req) {
        String executionId = executionService.start(
                req.getPipelineKey(),
                req.getPipelineVersion(),
                req.getInputParams(),
                req.getTriggerType(),
                req.getTriggeredBy(),
                req.getZoneCode());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executionId", executionId);
        result.put("message", "流程已启动，稍后调用 /progress 查看进度");
        return CommonResult.success(result);
    }

    /**
     * 查询流程执行进度
     * GET /admin-api/flow/test/progress?executionId=xxx
     */
    @Operation(summary = "查询流程执行进度")
    @GetMapping("/progress")
    public CommonResult<Map<String, Object>> progress(@RequestParam String executionId) {
        // 查主记录
        PipelineExecutionDO execution = executionMapper.selectByExecutionId(executionId);
        if (execution == null) {
            return CommonResult.error(404, "执行实例不存在: " + executionId);
        }

        // 查所有节点最新状态
        List<StepExecutionDO> steps = stepMapper.selectLatestByExecutionId(executionId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executionId", execution.getExecutionId());
        result.put("pipelineKey", execution.getPipelineKey());
        result.put("status", execution.getStatus());
        result.put("startedAt", execution.getStartedAt());
        result.put("finishedAt", execution.getFinishedAt());
        result.put("totalMs", execution.getTotalMs());

        // 节点进度列表
        List<Map<String, Object>> nodeList = steps.stream().map(s -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("nodeId", s.getNodeId());
            n.put("stepType", s.getStepType());
            n.put("status", s.getStatus());
            n.put("attempt", s.getAttempt());
            n.put("executeMs", s.getExecuteMs());
            n.put("errorMsg", s.getErrorMsg());
            n.put("outputData", s.getOutputData());
            return n;
        }).collect(Collectors.toList());

        result.put("nodes", nodeList);

        // 统计
        Map<String, Long> statusCount = steps.stream()
                .collect(Collectors.groupingBy(StepExecutionDO::getStatus,
                        Collectors.counting()));
        result.put("statusSummary", statusCount);

        return CommonResult.success(result);
    }
}