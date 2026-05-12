package com.librax.lab.module.flow.controller.admin.importer;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.flow.importer.PipelineImportResult;
import com.librax.lab.module.flow.importer.PipelineImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 流程文件导入")
@RestController
@RequestMapping("/flow/pipeline/import")
@Validated
public class PipelineImportController {

    @Resource
    private PipelineImportService importService;

    /**
     * 第一步：解析文件，返回预览（不写库）
     * 返回 List，单流程 size=1，多流程 size=N
     */
    @PostMapping("/preview")
    @Operation(summary = "解析流程文件，返回预览结果列表（不写库）")
    @PreAuthorize("@ss.hasPermission('flow:pipeline:import')")
    public CommonResult<List<PipelineImportResult>> preview(
            @RequestParam("file") @NotNull MultipartFile file) {
        return success(importService.preview(file));
    }

    /**
     * 第二步：确认导入，批量写库
     * 前端把 preview 返回的整个列表传回
     */
    @PostMapping("/confirm")
    @Operation(summary = "确认导入，批量写入数据库")
    @PreAuthorize("@ss.hasPermission('flow:pipeline:import')")
    public CommonResult<Boolean> confirm(@RequestBody ConfirmReqVO req) {
        importService.confirm(req.getResults());
        return success(true);
    }

    @Data
    public static class ConfirmReqVO {
        /** preview 返回的完整列表，原样传回 */
        @NotNull
        private List<PipelineImportResult> results;
    }
}