package com.librax.lab.module.flow.importer.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.librax.lab.module.flow.importer.PipelineImportResult;
import com.librax.lab.module.flow.importer.PipelineImportResult.StepPayload;
import com.librax.lab.module.flow.importer.PipelineImportResult.ValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * YAML 流程解析器 v2
 *
 * 支持两种格式自动识别：
 *   - 单流程：pipeline + steps 在顶层
 *   - 多流程：pipelines[] 数组，每个元素包含 pipeline + steps
 *
 * 返回 List<PipelineImportResult>，单流程时 size=1
 */
@Slf4j
@Component
public class PipelineYamlParser {

    private static final ObjectMapper YAML_MAPPER =
            new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final Set<String> VALID_TYPES = Set.of(
            "INSTRUMENT", "COMPUTE", "CONDITION", "MANUAL",
            "WAIT", "UNIT_LAUNCHER", "NOTIFY"
    );
    private static final Set<String> VALID_ON_FAILURE = Set.of(
            "FAIL_FAST", "SKIP", "CONTINUE_ON_FAIL"
    );

    // ── 主入口：返回列表（单流程 size=1，多流程 size=N）────

    public List<PipelineImportResult> parse(InputStream yamlInput) {
        MultiPipelineYamlDef multi;
        try {
            multi = YAML_MAPPER.readValue(yamlInput, MultiPipelineYamlDef.class);
        } catch (Exception e) {
            log.warn("[YamlParser] YAML 格式错误: {}", e.getMessage());
            return List.of(PipelineImportResult.builder()
                    .importable(false)
                    .errors(List.of(err(null, null, "YAML 格式错误: " + e.getMessage())))
                    .build());
        }

        List<PipelineYamlDef> defs = multi.toList();
        if (defs.isEmpty()) {
            return List.of(PipelineImportResult.builder()
                    .importable(false)
                    .errors(List.of(err(null, null, "文件为空或格式不正确，未找到任何流程定义")))
                    .build());
        }

        // 多流程时：收集所有 pipelineKey 用于跨流程引用校验
        Set<String> allKeys = defs.stream()
                .filter(d -> d.getPipeline() != null && d.getPipeline().getKey() != null)
                .map(d -> d.getPipeline().getKey())
                .collect(Collectors.toSet());

        return defs.stream()
                .map(def -> parseSingle(def, allKeys))
                .collect(Collectors.toList());
    }

    // ── 单流程解析 ────────────────────────────────────────

    private PipelineImportResult parseSingle(PipelineYamlDef def, Set<String> allPipelineKeys) {
        List<ValidationIssue> errors   = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        validatePipelineMeta(def.getPipeline(), errors);

        if (def.getSteps() == null || def.getSteps().isEmpty()) {
            errors.add(err(null, "steps", "steps 列表不能为空"));
            return buildResult(def, List.of(), errors, warnings);
        }

        fillDefaults(def);

        for (PipelineYamlDef.StepDef step : def.getSteps()) {
            validateStep(step, errors, warnings, allPipelineKeys);
        }

        validateDag(def.getSteps(), errors);

        List<StepPayload> payloads = toPayloads(def);
        return buildResult(def, payloads, errors, warnings);
    }

    // ── 默认值填充 ────────────────────────────────────────

    private void fillDefaults(PipelineYamlDef def) {
        String globalZone      = def.getPipeline().getZone();
        Long   globalTimeout   = def.getPipeline().getTimeoutMs();
        String globalOnFailure = def.getPipeline().getOnFailure();

        for (int i = 0; i < def.getSteps().size(); i++) {
            PipelineYamlDef.StepDef step = def.getSteps().get(i);

            if (step.getDispatch() == null) {
                step.setDispatch(
                        ("INSTRUMENT".equals(step.getType()) || "MANUAL".equals(step.getType()))
                                ? "QUEUED" : "DIRECT"
                );
            }
            if (step.getResource() != null && step.getResource().getZone() == null) {
                step.getResource().setZone(globalZone);
            }
            if (step.getTimeoutMs() == null) step.setTimeoutMs(globalTimeout);
            if (step.getOnFailure() == null) step.setOnFailure(globalOnFailure);
            if (step.getSortOrder() == null || step.getSortOrder() == 0) {
                step.setSortOrder(i * 10);
            }
        }
    }

    // ── 节点校验 ──────────────────────────────────────────

    private void validatePipelineMeta(PipelineYamlDef.PipelineMeta meta,
                                      List<ValidationIssue> errors) {
        if (meta == null) { errors.add(err(null, "pipeline", "pipeline 块缺失")); return; }
        if (isBlank(meta.getKey()))
            errors.add(err(null, "pipeline.key", "pipeline.key 为必填项"));
        if (meta.getVersion() == null || meta.getVersion() < 1)
            errors.add(err(null, "pipeline.version", "pipeline.version 必须是正整数"));
        if (!List.of("DRAFT","ACTIVE","INACTIVE").contains(meta.getStatus()))
            errors.add(err(null, "pipeline.status",
                    "status 必须是 DRAFT/ACTIVE/INACTIVE，当前: " + meta.getStatus()));
    }

    private void validateStep(PipelineYamlDef.StepDef step,
                              List<ValidationIssue> errors,
                              List<ValidationIssue> warnings,
                              Set<String> allPipelineKeys) {
        String id = step.getId();
        if (isBlank(id)) { errors.add(err(null, "id", "步骤缺少 id")); return; }

        if (isBlank(step.getType())) {
            errors.add(err(id, "type", "type 为必填项"));
        } else if (!VALID_TYPES.contains(step.getType())) {
            errors.add(err(id, "type", "type 非法: " + step.getType()));
        }

        if (isBlank(step.getStepKey())) {
            warnings.add(warn(id, "step_key", "未填 step_key，引擎将使用默认配置"));
        }

        if ("CONDITION".equals(step.getType())) {
            var cond = step.getCondition();
            if (cond == null) {
                errors.add(err(id, "condition", "CONDITION 节点必须有 condition 块"));
            } else {
                if (isBlank(cond.getExpr()))
                    errors.add(err(id, "condition.expr", "条件表达式 expr 为必填"));
                if (isBlank(cond.getTrueTo()) && isBlank(cond.getFalseTo()))
                    errors.add(err(id, "condition", "至少需要配置 true_to 或 false_to"));
            }
        }

        if ("UNIT_LAUNCHER".equals(step.getType())) {
            var unit = step.getUnit();
            if (unit == null) {
                errors.add(err(id, "unit", "UNIT_LAUNCHER 必须有 unit 块"));
            } else {
                if (isBlank(unit.getPipelineKey())) {
                    errors.add(err(id, "unit.pipeline_key", "unit.pipeline_key 为必填"));
                } else if (!allPipelineKeys.isEmpty()
                        && !allPipelineKeys.contains(unit.getPipelineKey())) {
                    // 在当前文件里找不到子流程 key，给 warn 而非 error（子流程可能单独导入过）
                    warnings.add(warn(id, "unit.pipeline_key",
                            "子流程 [" + unit.getPipelineKey() + "] 不在本文件中，"
                                    + "请确认已单独导入或在 pipelines[] 中声明"));
                }
            }
        }

        if ("INSTRUMENT".equals(step.getType()) && step.getResource() == null) {
            warnings.add(warn(id, "resource", "INSTRUMENT 节点未配置 resource，不做资源调度"));
        }

        if ("DIRECT".equals(step.getDispatch())
                && ("COMPUTE".equals(step.getType()) || "NOTIFY".equals(step.getType()))
                && isBlank(step.getBean())) {
            warnings.add(warn(id, "bean", "DIRECT 节点未指定 bean，将降级 MockExecutor"));
        }

        if (!isBlank(step.getOnFailure()) && !VALID_ON_FAILURE.contains(step.getOnFailure())) {
            errors.add(err(id, "on_failure", "on_failure 非法: " + step.getOnFailure()));
        }
    }

    // ── DAG 校验 ──────────────────────────────────────────

    private void validateDag(List<PipelineYamlDef.StepDef> steps,
                             List<ValidationIssue> errors) {
        Map<String, PipelineYamlDef.StepDef> byId = new LinkedHashMap<>();
        for (var step : steps) {
            if (step.getId() == null) continue;
            if (byId.containsKey(step.getId()))
                errors.add(err(step.getId(), "id", "节点 id 重复: " + step.getId()));
            byId.put(step.getId(), step);
        }

        for (var step : steps) {
            for (String dep : toDependsList(step.getDependsOn())) {
                if (!byId.containsKey(dep))
                    errors.add(err(step.getId(), "depends_on",
                            "依赖节点 [" + dep + "] 不存在"));
            }
            if (step.getCondition() != null) {
                var cond = step.getCondition();
                if (!isBlank(cond.getTrueTo()) && !byId.containsKey(cond.getTrueTo()))
                    errors.add(err(step.getId(), "condition.true_to",
                            "true_to [" + cond.getTrueTo() + "] 不存在"));
                if (!isBlank(cond.getFalseTo()) && !byId.containsKey(cond.getFalseTo()))
                    errors.add(err(step.getId(), "condition.false_to",
                            "false_to [" + cond.getFalseTo() + "] 不存在"));
            }
        }

        // 只在没有 depends_on 错误时才做环形检测
        boolean hasDepsError = errors.stream().anyMatch(e -> "depends_on".equals(e.getField()));
        if (!hasDepsError) detectCycle(byId, errors);
    }

    private void detectCycle(Map<String, PipelineYamlDef.StepDef> byId,
                             List<ValidationIssue> errors) {
        Map<String, Set<String>> outEdges = new HashMap<>();
        Map<String, Integer>     inDegree = new HashMap<>();
        byId.keySet().forEach(id -> { outEdges.put(id, new HashSet<>()); inDegree.put(id, 0); });

        for (var step : byId.values()) {
            for (String dep : toDependsList(step.getDependsOn())) {
                if (byId.containsKey(dep)) {
                    outEdges.get(dep).add(step.getId());
                    inDegree.merge(step.getId(), 1, Integer::sum);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        inDegree.forEach((id, deg) -> { if (deg == 0) queue.offer(id); });
        int visited = 0;
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            visited++;
            for (String next : outEdges.get(cur)) {
                if (inDegree.merge(next, -1, Integer::sum) == 0) queue.offer(next);
            }
        }
        if (visited < byId.size()) {
            errors.add(err(null, "depends_on",
                    "检测到环形依赖，涉及节点数: " + (byId.size() - visited)));
        }
    }

    // ── 转换为 StepPayload ────────────────────────────────

    private List<StepPayload> toPayloads(PipelineYamlDef def) {
        String pKey = def.getPipeline().getKey();
        int    ver  = def.getPipeline().getVersion();

        return def.getSteps().stream().map(step -> {
            var cond = step.getCondition();
            var unit = step.getUnit();
            var res  = step.getResource();

            Map<String, Object> params = new LinkedHashMap<>();
            if (step.getParams() != null) params.putAll(step.getParams());
            if (unit != null) {
                params.put("unitPipelineKey", unit.getPipelineKey());
                if (unit.getPipelineVersion() != null)
                    params.put("unitPipelineVersion", unit.getPipelineVersion());
                params.put("maxRetry", unit.getMaxRetry());
            }
            if (!isBlank(step.getBean())) params.put("beanName", step.getBean());

            return StepPayload.builder()
                    .pipelineKey(pKey)
                    .pipelineVersion(ver)
                    .nodeId(step.getId())
                    .stepKey(step.getStepKey())
                    .stepType(step.getType())
                    .name(step.getName())
                    .dispatchMode(step.getDispatch())
                    .taskType(inferTaskType(step.getType()))
                    .resourceEnabled(res != null ? 1 : 0)
                    .zoneCode(res != null ? res.getZone() : null)
                    .dependsOn(toJson(toDependsList(step.getDependsOn())))
                    .conditionExpr(cond != null ? cond.getExpr() : null)
                    .trueBranch(cond != null ? cond.getTrueTo() : null)
                    .falseBranch(cond != null ? cond.getFalseTo() : null)
                    .paramsOverride(params.isEmpty() ? null : toJson(params))
                    .inputMapping(toJson(step.getInputMapping()))
                    .outputMapping(toJson(step.getOutputMapping()))
                    .timeoutMs(step.getTimeoutMs())
                    .maxAttempts(step.getMaxAttempts())
                    .backoffMs(step.getBackoffMs())
                    .onFailure(step.getOnFailure())
                    .sortOrder(step.getSortOrder())
                    .runnableStandalone(1)
                    .build();
        }).collect(Collectors.toList());
    }

    private String inferTaskType(String type) {
        return switch (type) {
            case "INSTRUMENT" -> "INSTRUMENT";
            case "MANUAL"     -> "MANUAL";
            default           -> "COMPUTE";
        };
    }

    // ── 工具 ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<String> toDependsList(Object dependsOn) {
        if (dependsOn == null) return List.of();
        if (dependsOn instanceof String s)
            return isBlank(s) ? List.of() : List.of(s.trim());
        if (dependsOn instanceof List<?> list)
            return list.stream().filter(Objects::nonNull)
                    .map(Object::toString).map(String::trim).collect(Collectors.toList());
        return List.of();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map<?,?> m && m.isEmpty()) return null;
        if (obj instanceof List<?> l && l.isEmpty()) return "[]";
        try { return JSON_MAPPER.writeValueAsString(obj); } catch (Exception e) { return null; }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private ValidationIssue err(String nodeId, String field, String message) {
        return ValidationIssue.builder().level("ERROR").nodeId(nodeId).field(field).message(message).build();
    }
    private ValidationIssue warn(String nodeId, String field, String message) {
        return ValidationIssue.builder().level("WARN").nodeId(nodeId).field(field).message(message).build();
    }

    private PipelineImportResult buildResult(PipelineYamlDef def,
                                             List<StepPayload> payloads,
                                             List<ValidationIssue> errors,
                                             List<ValidationIssue> warnings) {
        var meta = def.getPipeline();
        return PipelineImportResult.builder()
                .pipelineKey(meta != null ? meta.getKey() : null)
                .version(meta != null ? meta.getVersion() : null)
                .name(meta != null ? meta.getName() : null)
                .steps(payloads)
                .errors(errors)
                .warnings(warnings)
                .importable(errors.isEmpty())
                .build();
    }
}