package com.librax.lab.module.flow.controller.admin.pipelineexecution;

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

import com.librax.lab.module.flow.controller.admin.pipelineexecution.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.service.pipelineexecution.PipelineExecutionService;

@Tag(name = "管理后台 - 流程执行实例，支持完整流程、节点单独运行、补偿执行")
@RestController
@RequestMapping("/flow/pipeline-execution")
@Validated
public class PipelineExecutionController {

    @Resource
    private PipelineExecutionService pipelineExecutionService;

    @PostMapping("/create")
    @Operation(summary = "创建流程执行实例，支持完整流程、节点单独运行、补偿执行")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-execution:create')")
    public CommonResult<Long> createPipelineExecution(@Valid @RequestBody PipelineExecutionSaveReqVO createReqVO) {
        return success(pipelineExecutionService.createPipelineExecution(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新流程执行实例，支持完整流程、节点单独运行、补偿执行")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-execution:update')")
    public CommonResult<Boolean> updatePipelineExecution(@Valid @RequestBody PipelineExecutionSaveReqVO updateReqVO) {
        pipelineExecutionService.updatePipelineExecution(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除流程执行实例，支持完整流程、节点单独运行、补偿执行")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:pipeline-execution:delete')")
    public CommonResult<Boolean> deletePipelineExecution(@RequestParam("id") Long id) {
        pipelineExecutionService.deletePipelineExecution(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除流程执行实例，支持完整流程、节点单独运行、补偿执行")
                @PreAuthorize("@ss.hasPermission('flow:pipeline-execution:delete')")
    public CommonResult<Boolean> deletePipelineExecutionList(@RequestParam("ids") List<Long> ids) {
        pipelineExecutionService.deletePipelineExecutionListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得流程执行实例，支持完整流程、节点单独运行、补偿执行")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-execution:query')")
    public CommonResult<PipelineExecutionRespVO> getPipelineExecution(@RequestParam("id") Long id) {
        PipelineExecutionDO pipelineExecution = pipelineExecutionService.getPipelineExecution(id);
        return success(BeanUtils.toBean(pipelineExecution, PipelineExecutionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得流程执行实例，支持完整流程、节点单独运行、补偿执行分页")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-execution:query')")
    public CommonResult<PageResult<PipelineExecutionRespVO>> getPipelineExecutionPage(@Valid PipelineExecutionPageReqVO pageReqVO) {
        PageResult<PipelineExecutionDO> pageResult = pipelineExecutionService.getPipelineExecutionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, PipelineExecutionRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出流程执行实例，支持完整流程、节点单独运行、补偿执行 Excel")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-execution:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPipelineExecutionExcel(@Valid PipelineExecutionPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PipelineExecutionDO> list = pipelineExecutionService.getPipelineExecutionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "流程执行实例，支持完整流程、节点单独运行、补偿执行.xls", "数据", PipelineExecutionRespVO.class,
                        BeanUtils.toBean(list, PipelineExecutionRespVO.class));
    }

}