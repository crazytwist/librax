package com.librax.lab.module.flow.controller.admin.pipelinestep;

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

import com.librax.lab.module.flow.controller.admin.pipelinestep.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinestep.PipelineStepDO;
import com.librax.lab.module.flow.service.pipelinestep.PipelineStepService;

@Tag(name = "管理后台 - 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]")
@RestController
@RequestMapping("/flow/pipeline-step")
@Validated
public class PipelineStepController {

    @Resource
    private PipelineStepService pipelineStepService;

    @PostMapping("/create")
    @Operation(summary = "创建流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-step:create')")
    public CommonResult<Long> createPipelineStep(@Valid @RequestBody PipelineStepSaveReqVO createReqVO) {
        return success(pipelineStepService.createPipelineStep(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-step:update')")
    public CommonResult<Boolean> updatePipelineStep(@Valid @RequestBody PipelineStepSaveReqVO updateReqVO) {
        pipelineStepService.updatePipelineStep(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:pipeline-step:delete')")
    public CommonResult<Boolean> deletePipelineStep(@RequestParam("id") Long id) {
        pipelineStepService.deletePipelineStep(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]")
                @PreAuthorize("@ss.hasPermission('flow:pipeline-step:delete')")
    public CommonResult<Boolean> deletePipelineStepList(@RequestParam("ids") List<Long> ids) {
        pipelineStepService.deletePipelineStepListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-step:query')")
    public CommonResult<PipelineStepRespVO> getPipelineStep(@RequestParam("id") Long id) {
        PipelineStepDO pipelineStep = pipelineStepService.getPipelineStep(id);
        return success(BeanUtils.toBean(pipelineStep, PipelineStepRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]分页")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-step:query')")
    public CommonResult<PageResult<PipelineStepRespVO>> getPipelineStepPage(@Valid PipelineStepPageReqVO pageReqVO) {
        PageResult<PipelineStepDO> pageResult = pipelineStepService.getPipelineStepPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, PipelineStepRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_] Excel")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-step:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPipelineStepExcel(@Valid PipelineStepPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PipelineStepDO> list = pipelineStepService.getPipelineStepPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_].xls", "数据", PipelineStepRespVO.class,
                        BeanUtils.toBean(list, PipelineStepRespVO.class));
    }

}