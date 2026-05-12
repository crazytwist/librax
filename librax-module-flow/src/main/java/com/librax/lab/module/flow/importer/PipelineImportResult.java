package com.librax.lab.module.flow.importer;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果：解析完成后返回给前端
 * 包含解析出的流程元信息、节点列表、校验错误、警告
 */
@Data
@Builder
public class PipelineImportResult {

    // 解析的流程 key（前端用来渲染预览）
    private String pipelineKey;
    private Integer version;
    private String name;

    // 解析后的节点（直接对应 pd_pipeline_step 的写入格式）
    private List<StepPayload> steps;

    // 校验问题
    @Builder.Default
    private List<ValidationIssue> errors = new ArrayList<>();

    @Builder.Default
    private List<ValidationIssue> warnings = new ArrayList<>();

    // 是否可以直接导入（无 error 时为 true）
    private boolean importable;

    // ── 内部类 ──────────────────────────────────────────

    @Data
    @Builder
    public static class StepPayload {
        private String nodeId;
        private String stepKey;
        private String stepType;
        private String name;
        private String dispatchMode;
        private String taskType;
        private Integer resourceEnabled;
        private String zoneCode;
        private String dependsOn;          // JSON 数组字符串
        private String conditionExpr;
        private String trueBranch;
        private String falseBranch;
        private String paramsOverride;     // JSON 字符串
        private String inputMapping;       // JSON 字符串
        private String outputMapping;      // JSON 字符串
        private Long timeoutMs;
        private Integer maxAttempts;
        private Long backoffMs;
        private String onFailure;
        private Integer sortOrder;
        private Integer runnableStandalone;
        private String pipelineKey;
        private Integer pipelineVersion;
    }

    @Data
    @Builder
    public static class ValidationIssue {
        private String level;   // ERROR | WARN
        private String nodeId;  // null 表示流程级别的问题
        private String field;
        private String message;
    }
}