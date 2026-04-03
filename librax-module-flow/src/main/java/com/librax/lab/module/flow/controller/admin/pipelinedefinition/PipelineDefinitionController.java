package com.librax.lab.module.flow.controller.admin.pipelinedefinition;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import static com.librax.lab.framework.common.pojo.CommonResult.success;

import com.librax.lab.framework.excel.core.util.ExcelUtils;

import com.librax.lab.framework.apilog.core.annotation.ApiAccessLog;
import static com.librax.lab.framework.apilog.core.enums.OperateTypeEnum.*;

import com.librax.lab.module.flow.controller.admin.pipelinedefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinedefinition.PipelineDefinitionDO;
import com.librax.lab.module.flow.service.pipelinedefinition.PipelineDefinitionService;

@Tag(name = "管理后台 - 流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]")
@RestController
@RequestMapping("/flow/pipeline-definition")
@Validated
public class PipelineDefinitionController {

    @Resource
    private PipelineDefinitionService pipelineDefinitionService;

    @PostMapping("/create")
    @Operation(summary = "创建流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-definition:create')")
    public CommonResult<Long> createPipelineDefinition(@Valid @RequestBody PipelineDefinitionSaveReqVO createReqVO) {
        return success(pipelineDefinitionService.createPipelineDefinition(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-definition:update')")
    public CommonResult<Boolean> updatePipelineDefinition(@Valid @RequestBody PipelineDefinitionSaveReqVO updateReqVO) {
        pipelineDefinitionService.updatePipelineDefinition(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:pipeline-definition:delete')")
    public CommonResult<Boolean> deletePipelineDefinition(@RequestParam("id") Long id) {
        pipelineDefinitionService.deletePipelineDefinition(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]")
                @PreAuthorize("@ss.hasPermission('flow:pipeline-definition:delete')")
    public CommonResult<Boolean> deletePipelineDefinitionList(@RequestParam("ids") List<Long> ids) {
        pipelineDefinitionService.deletePipelineDefinitionListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-definition:query')")
    public CommonResult<PipelineDefinitionRespVO> getPipelineDefinition(@RequestParam("id") Long id) {
        PipelineDefinitionDO pipelineDefinition = pipelineDefinitionService.getPipelineDefinition(id);
        return success(BeanUtils.toBean(pipelineDefinition, PipelineDefinitionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_]分页")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-definition:query')")
    public CommonResult<PageResult<PipelineDefinitionRespVO>> getPipelineDefinitionPage(@Valid PipelineDefinitionPageReqVO pageReqVO) {
        PageResult<PipelineDefinitionDO> pageResult = pipelineDefinitionService.getPipelineDefinitionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, PipelineDefinitionRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_] Excel")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-definition:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPipelineDefinitionExcel(@Valid PipelineDefinitionPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PipelineDefinitionDO> list = pipelineDefinitionService.getPipelineDefinitionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "流程定义表，存元信息和全局配置，步骤编排见 pd_pipeline_step [pd_].xls", "数据", PipelineDefinitionRespVO.class,
                        BeanUtils.toBean(list, PipelineDefinitionRespVO.class));
    }

}