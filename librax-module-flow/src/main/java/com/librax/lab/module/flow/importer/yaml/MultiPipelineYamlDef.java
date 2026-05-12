package com.librax.lab.module.flow.importer.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 多流程 YAML 顶层结构
 *
 * 支持两种格式，解析器自动识别：
 *
 * 格式一（单流程，原有格式）：
 *   pipeline:
 *     key: xxx
 *   steps: [...]
 *
 * 格式二（多流程，新格式）：
 *   pipelines:
 *     - pipeline:
 *         key: xxx
 *       steps: [...]
 *     - pipeline:
 *         key: yyy
 *       steps: [...]
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiPipelineYamlDef {

    /** 多流程格式：pipelines[] */
    private List<PipelineYamlDef> pipelines;

    /** 单流程格式兼容：pipeline + steps 直接在顶层 */
    private PipelineYamlDef.PipelineMeta pipeline;
    private java.util.List<PipelineYamlDef.StepDef> steps;

    /**
     * 统一转成 List<PipelineYamlDef>，无论哪种格式
     */
    public List<PipelineYamlDef> toList() {
        // 多流程格式
        if (pipelines != null && !pipelines.isEmpty()) {
            return pipelines;
        }
        // 单流程格式：组装成 PipelineYamlDef
        if (pipeline != null) {
            PipelineYamlDef single = new PipelineYamlDef();
            single.setPipeline(pipeline);
            single.setSteps(steps);
            return List.of(single);
        }
        return List.of();
    }
}