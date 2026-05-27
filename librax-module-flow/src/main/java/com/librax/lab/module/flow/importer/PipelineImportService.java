package com.librax.lab.module.flow.importer;

import com.librax.lab.module.flow.controller.admin.pipelinestep.vo.PipelineStepSaveReqVO;
import com.librax.lab.module.flow.importer.yaml.PipelineYamlParser;
import com.librax.lab.module.flow.service.pipelinedefinition.PipelineDefinitionService;
import com.librax.lab.module.flow.service.pipelinestep.PipelineStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 流程导入 Service v2
 * preview / confirm 统一处理多流程列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineImportService {

    private final PipelineYamlParser            yamlParser;
    private final PipelineDefinitionService     definitionService;
    private final PipelineStepService           stepService;

    // ── preview：解析文件，返回列表（不写库）─────────────

    public List<PipelineImportResult> preview(MultipartFile file) {
        String filename = file.getOriginalFilename() == null
                ? "" : file.getOriginalFilename().toLowerCase();
        try (InputStream is = file.getInputStream()) {
            if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                return yamlParser.parse(is);
            }
            return List.of(PipelineImportResult.builder()
                    .importable(false)
                    .errors(List.of(PipelineImportResult.ValidationIssue.builder()
                            .level("ERROR")
                            .message("不支持的文件格式: " + filename + "，当前支持 .yaml / .yml")
                            .build()))
                    .build());
        } catch (Exception e) {
            log.error("[ImportService] 文件解析异常", e);
            return List.of(PipelineImportResult.builder()
                    .importable(false)
                    .errors(List.of(PipelineImportResult.ValidationIssue.builder()
                            .level("ERROR")
                            .message("文件读取失败: " + e.getMessage())
                            .build()))
                    .build());
        }
    }

    /**
     * 解析 YAML 文本内容（用于在线编辑器实时校验）
     */
    public List<PipelineImportResult> preview(String yamlContent) {
        try {
            return yamlParser.parse(yamlContent);
        } catch (Exception e) {
            log.error("[ImportService] YAML 文本解析异常", e);
            return List.of(PipelineImportResult.builder()
                    .importable(false)
                    .errors(List.of(PipelineImportResult.ValidationIssue.builder()
                            .level("ERROR")
                            .message("YAML 解析失败: " + e.getMessage())
                            .build()))
                    .build());
        }
    }

    // ── confirm：批量写库 ─────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public void confirm(List<PipelineImportResult> results) {
        for (PipelineImportResult result : results) {
            if (!result.isImportable()) {
                throw new IllegalStateException(
                        "流程 [" + result.getPipelineKey() + "] 存在校验错误，无法导入");
            }
            confirmSingle(result.getPipelineKey(), result.getVersion(), result.getSteps());
        }
    }

    private void confirmSingle(String pipelineKey, Integer version,
                               List<PipelineImportResult.StepPayload> steps) {
        log.info("[ImportService] 导入 pipelineKey={} version={} stepCount={}",
                pipelineKey, version, steps.size());
        ensurePipelineDefinition(pipelineKey, version);
        List<PipelineStepSaveReqVO> saveReqVOS =
                cn.hutool.core.bean.BeanUtil.copyToList(steps, PipelineStepSaveReqVO.class);
        stepService.saveBatch(pipelineKey, version, saveReqVOS);
        log.info("[ImportService] 导入完成 pipelineKey={} version={}", pipelineKey, version);
    }

    private void ensurePipelineDefinition(String pipelineKey, Integer version) {
        if (!definitionService.existsByKeyAndVersion(pipelineKey, version)) {
            definitionService.createDraft(pipelineKey, version);
            log.info("[ImportService] 自动创建 pipeline_definition key={} v={}", pipelineKey, version);
        }
    }
}