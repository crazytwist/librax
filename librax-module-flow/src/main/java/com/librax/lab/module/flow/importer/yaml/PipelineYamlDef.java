package com.librax.lab.module.flow.importer.yaml;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * YAML 文件顶层映射模型
 * 对应 water_quality_test.yaml 的完整结构
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineYamlDef {

    private PipelineMeta pipeline;
    private List<StepDef> steps;

    // ──────────────────────────────────────────────────────
    //  pipeline 块
    // ──────────────────────────────────────────────────────
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PipelineMeta {
        private String key;
        private Integer version;
        private String name;
        private String description;
        private String status = "DRAFT";
        private String zone;

        @JsonAlias("timeout_ms")
        private Long timeoutMs = 3_600_000L;

        @JsonAlias("on_failure")
        private String onFailure = "FAIL_FAST";
    }

    // ──────────────────────────────────────────────────────
    //  steps[] 块
    // ──────────────────────────────────────────────────────
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepDef {

        private String id;
        private String name;
        private String type;                    // INSTRUMENT/COMPUTE/CONDITION/MANUAL/WAIT/UNIT_LAUNCHER/NOTIFY
        private String dispatch;                // QUEUED | DIRECT，缺省按 type 推断
        private String bean;                    // DIRECT+COMPUTE 时的 Spring Bean 名

        @JsonAlias("step_key")
        private String stepKey;

        @JsonAlias("depends_on")
        private Object dependsOn;               // 字符串或字符串数组，统一在解析时转 List

        private ResourceDef resource;
        private ConditionDef condition;         // CONDITION 节点专属
        private UnitDef unit;                   // UNIT_LAUNCHER 节点专属

        private Map<String, Object> params;

        @JsonAlias("input_mapping")
        private Map<String, String> inputMapping;

        @JsonAlias("output_mapping")
        private Map<String, String> outputMapping;

        @JsonAlias("timeout_ms")
        private Long timeoutMs;

        @JsonAlias("max_attempts")
        private Integer maxAttempts = 1;

        @JsonAlias("backoff_ms")
        private Long backoffMs = 0L;

        @JsonAlias("sort_order")
        private Integer sortOrder = 0;

        @JsonAlias("on_failure")
        private String onFailure;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceDef {
        private String type;
        private String zone;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConditionDef {
        private String expr;

        @JsonAlias("true_to")
        private String trueTo;

        @JsonAlias("false_to")
        private String falseTo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnitDef {

        @JsonAlias("pipeline_key")
        private String pipelineKey;

        @JsonAlias("pipeline_version")
        private Integer pipelineVersion;

        @JsonAlias("max_retry")
        private Integer maxRetry = 3;
    }
}