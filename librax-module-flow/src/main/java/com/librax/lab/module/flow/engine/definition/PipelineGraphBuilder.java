package com.librax.lab.module.flow.engine.definition;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.flow.dal.dataobject.pipelinedefinition.PipelineDefinitionDO;
import com.librax.lab.module.flow.dal.dataobject.pipelinestep.PipelineStepDO;
import com.librax.lab.module.flow.dal.dataobject.stepdefinition.StepDefinitionDO;
import com.librax.lab.module.flow.dal.mysql.pipelinedefinition.PipelineDefinitionMapper;
import com.librax.lab.module.flow.dal.mysql.pipelinestep.PipelineStepMapper;
import com.librax.lab.module.flow.dal.mysql.stepdefinition.StepDefinitionMapper;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.enums.FailStrategyEnum;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 流程图组装器
 *
 * <p>职责：从三张定义表读取数据，组装成引擎运行时使用的 {@link PipelineGraph}。
 * <p>固定 3 次 DB 查询，不随节点数量增长：
 * <ol>
 *   <li>pd_pipeline_definition × 1
 *   <li>pd_pipeline_step × 1（按流程版本批量）
 *   <li>pd_step_definition × 1（按 stepKey 去重后批量查最新版）
 *      + 最多 N 次单条查询（仅当节点指定了具体 step_version 时）
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineGraphBuilder {

    private final PipelineDefinitionMapper definitionMapper;
    private final PipelineStepMapper stepMapper;
    private final StepDefinitionMapper stepDefMapper;

    // ----------------------------------------------------------------
    // 公共入口
    // ----------------------------------------------------------------

    public PipelineGraph build(String pipelineKey, Integer version) {
        PipelineDefinitionDO definition = loadDefinition(pipelineKey, version);
        List<PipelineStepDO> pipelineSteps = loadPipelineSteps(pipelineKey, version);
        Map<String, StepDefinitionDO> stepDefMap = loadStepDefinitions(pipelineSteps);

        List<StepNode> stepNodes = pipelineSteps.stream()
                .map(ps -> assembleNode(ps, stepDefMap, definition))
                .collect(Collectors.toList());

        PipelineGraph graph = new PipelineGraph();
        graph.setPipelineKey(pipelineKey);
        graph.setVersion(version);
        graph.setName(definition.getName());
        graph.setFailStrategy(FailStrategyEnum.valueOf(definition.getFailStrategy()));
        graph.setSteps(stepNodes);
        graph.buildIndex();

        log.info("[PipelineGraphBuilder] 组装完成 pipeline_key={} version={} 节点数={}",
                pipelineKey, version, stepNodes.size());
        return graph;
    }

    // ----------------------------------------------------------------
    // Step 1：加载流程定义
    // ----------------------------------------------------------------

    private PipelineDefinitionDO loadDefinition(String pipelineKey, Integer version) {
        PipelineDefinitionDO definition = definitionMapper.selectByKeyAndVersion(pipelineKey, version);
        if (definition == null) {
            throw exception(PIPELINE_DEFINITION_NOT_EXISTS, pipelineKey, version);
        }
        if (!"ACTIVE".equals(definition.getStatus())) {
            throw exception(PIPELINE_DEFINITION_NOT_ACTIVE, pipelineKey, version, definition.getStatus());
        }
        return definition;
    }

    // ----------------------------------------------------------------
    // Step 2：加载流程节点列表
    // ----------------------------------------------------------------

    private List<PipelineStepDO> loadPipelineSteps(String pipelineKey, Integer version) {
        List<PipelineStepDO> steps = stepMapper.selectListByPipelineVersion(pipelineKey, version);
        if (CollectionUtils.isEmpty(steps)) {
            throw exception(PIPELINE_STEP_EMPTY, pipelineKey, version);
        }
        return steps;
    }

    // ----------------------------------------------------------------
    // Step 3：批量加载 step_definition
    // ----------------------------------------------------------------

    private Map<String, StepDefinitionDO> loadStepDefinitions(List<PipelineStepDO> pipelineSteps) {
        Map<String, StepDefinitionDO> result = new HashMap<>();

        // 3a. 批量查未指定版本的（取最新 ACTIVE）
        Set<String> keysForLatest = pipelineSteps.stream()
                .filter(ps -> ps.getStepVersion() == null)
                .map(PipelineStepDO::getStepKey)
                .collect(Collectors.toSet());

        if (!keysForLatest.isEmpty()) {
            stepDefMapper.selectLatestActiveByKeys(new ArrayList<>(keysForLatest))
                    .forEach(sd -> result.put(sd.getStepKey(), sd));
            keysForLatest.forEach(key -> {
                if (!result.containsKey(key)) {
                    throw exception(STEP_DEFINITION_NOT_EXISTS, key);
                }
            });
        }

        // 3b. 单条查指定了 step_version 的节点
        pipelineSteps.stream()
                .filter(ps -> ps.getStepVersion() != null)
                .forEach(ps -> {
                    String mapKey = versionedKey(ps.getStepKey(), ps.getStepVersion());
                    if (!result.containsKey(mapKey)) {
                        StepDefinitionDO sd = stepDefMapper.selectByKeyAndVersion(
                                ps.getStepKey(), ps.getStepVersion());
                        if (sd == null) {
                            throw exception(STEP_DEFINITION_VERSION_NOT_EXISTS,
                                    ps.getStepKey(), ps.getStepVersion());
                        }
                        result.put(mapKey, sd);
                    }
                });

        return result;
    }

    // ----------------------------------------------------------------
    // Step 4：单节点组装（三层参数合并）
    // ----------------------------------------------------------------

    private StepNode assembleNode(PipelineStepDO ps,
                                  Map<String, StepDefinitionDO> stepDefMap,
                                  PipelineDefinitionDO pd) {
        String defKey = ps.getStepVersion() != null
                ? versionedKey(ps.getStepKey(), ps.getStepVersion())
                : ps.getStepKey();
        StepDefinitionDO sd = stepDefMap.get(defKey);
        if (sd == null) {
            throw exception(STEP_DEFINITION_NOT_EXISTS, ps.getStepKey());
        }

        return StepNode.builder()
                // ── 节点身份 ──────────────────────────────────────────
                .pipelineStepId(ps.getId())
                .nodeId(ps.getNodeId())
                .stepKey(ps.getStepKey())
                .name(sd.getName())
                // 步骤类型：优先使用 pipeline_step 级别的覆盖，否则回退到 step_definition 的配置
                .stepType(StepTypeEnum.valueOf(
                        StringUtils.hasText(ps.getStepType()) ? ps.getStepType() : sd.getStepType()))
                // 加入分发模式 直连 还是 队列
                .dispatchMode(ps.getDispatchMode())
                .taskType(ps.getTaskType())
                // ── 编排关系 ──────────────────────────────────────────
                .dependsOn(parseList(ps.getDependsOn()))
                .conditionExpr(ps.getConditionExpr())
                .trueBranch(ps.getTrueBranch())
                .falseBranch(ps.getFalseBranch())
                .branches(parseStringMap(ps.getBranches()))
                // ── 执行配置（优先级：pipeline_step > step_def > pipeline_def）──
                .timeoutMs(coalesce(ps.getTimeoutMs(),
                        sd.getDefaultTimeoutMs(),
                        pd.getDefaultTimeoutMs()))
                .maxAttempts(coalesce(ps.getMaxAttempts(),
                        sd.getDefaultMaxAttempts(),
                        pd.getDefaultMaxAttempts(),
                        1))
                .backoffMs(coalesce(ps.getBackoffMs(),
                        sd.getDefaultBackoffMs(),
                        pd.getDefaultBackoffMs(),
                        2_000L))
                .onFailure(resolveFailStrategy(ps.getOnFailure(), pd.getFailStrategy()))
                // ── 参数 ──────────────────────────────────────────────
                .params(mergeParams(sd.getDefaultParams(), ps.getParamsOverride()))
                .inputMapping(parseStringMap(ps.getInputMapping()))
                .outputMapping(parseStringMap(ps.getOutputMapping()))
                // ── 补偿 ──────────────────────────────────────────────
                .compensateNodeId(ps.getCompensateNodeId())
                .compensateStepKey(coalesce(ps.getCompensateStepKey(), sd.getCompensateStepKey()))
                .compensateParams(parseObjectMap(
                        StringUtils.hasText(ps.getCompensateParams())
                                ? ps.getCompensateParams()
                                : sd.getCompensateParams()))
                .compensateOn(ps.getCompensateOn())
                // ── 单独运行 ──────────────────────────────────────────
                // 修正2：两个 DO 的 runnableStandalone 都是 Boolean，直接传 Boolean
                .runnableStandalone(resolveBoolean(ps.getRunnableStandalone(),
                        sd.getRunnableStandalone()))
                .mockOutput(parseObjectMap(
                        StringUtils.hasText(ps.getMockOutput())
                                ? ps.getMockOutput()
                                : sd.getMockOutput()))
                // ── COMPUTE 专用 ──────────────────────────────────────
                .executor(sd.getExecutor())
                .beanName(sd.getBeanName())
                .methodName(sd.getMethodName())
                .chainId(sd.getChainId())
                // ── INSTRUMENT 专用 ───────────────────────────────────
                .deviceType(sd.getDeviceType())
                .command(sd.getCommand())
                // 资源启用和所属区域编码
                .resourceEnabled(ps.getResourceEnabled())
                .zoneCode(ps.getZoneCode())
                // 资源等待超时（NULL 则由 StepSubmitter 使用默认值）
                .resourceWaitTimeoutMs(ps.getResourceWaitTimeoutMs())
                .build();
    }

    // ----------------------------------------------------------------
    // 工具方法
    // ----------------------------------------------------------------

    private List<String> parseList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        return JSON.parseObject(json, new TypeReference<List<String>>() {
        });
    }

    private Map<String, String> parseStringMap(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return JSON.parseObject(json, new TypeReference<Map<String, String>>() {
        });
    }

    private Map<String, Object> parseObjectMap(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
        });
    }

    private Map<String, Object> mergeParams(String defaultJson, String overrideJson) {
        Map<String, Object> merged = new HashMap<>();
        if (StringUtils.hasText(defaultJson)) {
            merged.putAll(JSON.parseObject(defaultJson,
                    new TypeReference<Map<String, Object>>() {
                    }));
        }
        if (StringUtils.hasText(overrideJson)) {
            merged.putAll(JSON.parseObject(overrideJson,
                    new TypeReference<Map<String, Object>>() {
                    }));
        }
        return merged;
    }

    @SafeVarargs
    private <T> T coalesce(T... values) {
        for (T v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private FailStrategyEnum resolveFailStrategy(String nodeOnFailure,
                                                 String pipelineFailStrategy) {
        String raw = StringUtils.hasText(nodeOnFailure) ? nodeOnFailure : pipelineFailStrategy;
        return FailStrategyEnum.valueOf(raw);
    }

    /**
     * 修正2：两个 DO 的 runnableStandalone 都是 Boolean，统一用 Boolean 入参
     */
    private boolean resolveBoolean(Boolean stepVal, Boolean defVal) {
        if (stepVal != null) {
            return stepVal;
        }
        if (defVal != null) {
            return defVal;
        }
        return true;
    }

    private String versionedKey(String stepKey, Integer version) {
        return stepKey + ":" + version;
    }
}