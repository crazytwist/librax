package com.librax.lab.module.flow.controller.admin.stepexecution;

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

import com.librax.lab.module.flow.controller.admin.stepexecution.vo.*;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.service.stepexecution.StepExecutionService;

@Tag(name = "管理后台 - 步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪")
@RestController
@RequestMapping("/flow/step-execution")
@Validated
public class StepExecutionController {

    @Resource
    private StepExecutionService stepExecutionService;

    @PostMapping("/create")
    @Operation(summary = "创建步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪")
    @PreAuthorize("@ss.hasPermission('flow:step-execution:create')")
    public CommonResult<Long> createStepExecution(@Valid @RequestBody StepExecutionSaveReqVO createReqVO) {
        return success(stepExecutionService.createStepExecution(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪")
    @PreAuthorize("@ss.hasPermission('flow:step-execution:update')")
    public CommonResult<Boolean> updateStepExecution(@Valid @RequestBody StepExecutionSaveReqVO updateReqVO) {
        stepExecutionService.updateStepExecution(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:step-execution:delete')")
    public CommonResult<Boolean> deleteStepExecution(@RequestParam("id") Long id) {
        stepExecutionService.deleteStepExecution(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪")
                @PreAuthorize("@ss.hasPermission('flow:step-execution:delete')")
    public CommonResult<Boolean> deleteStepExecutionList(@RequestParam("ids") List<Long> ids) {
        stepExecutionService.deleteStepExecutionListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:step-execution:query')")
    public CommonResult<StepExecutionRespVO> getStepExecution(@RequestParam("id") Long id) {
        StepExecutionDO stepExecution = stepExecutionService.getStepExecution(id);
        return success(BeanUtils.toBean(stepExecution, StepExecutionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪分页")
    @PreAuthorize("@ss.hasPermission('flow:step-execution:query')")
    public CommonResult<PageResult<StepExecutionRespVO>> getStepExecutionPage(@Valid StepExecutionPageReqVO pageReqVO) {
        PageResult<StepExecutionDO> pageResult = stepExecutionService.getStepExecutionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, StepExecutionRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪 Excel")
    @PreAuthorize("@ss.hasPermission('flow:step-execution:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStepExecutionExcel(@Valid StepExecutionPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StepExecutionDO> list = stepExecutionService.getStepExecutionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "步骤执行记录，每次重试独立一行，支持单独运行与补偿追踪.xls", "数据", StepExecutionRespVO.class,
                        BeanUtils.toBean(list, StepExecutionRespVO.class));
    }

}