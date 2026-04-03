package com.librax.lab.module.flow.controller.admin.stepdefinition;

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

import com.librax.lab.module.flow.controller.admin.stepdefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.stepdefinition.StepDefinitionDO;
import com.librax.lab.module.flow.service.stepdefinition.StepDefinitionService;

@Tag(name = "管理后台 - 步骤定义表，可复用的步骤组件库 [pd_]")
@RestController
@RequestMapping("/flow/step-definition")
@Validated
public class StepDefinitionController {

    @Resource
    private StepDefinitionService stepDefinitionService;

    @PostMapping("/create")
    @Operation(summary = "创建步骤定义表，可复用的步骤组件库 [pd_]")
    @PreAuthorize("@ss.hasPermission('flow:step-definition:create')")
    public CommonResult<Long> createStepDefinition(@Valid @RequestBody StepDefinitionSaveReqVO createReqVO) {
        return success(stepDefinitionService.createStepDefinition(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新步骤定义表，可复用的步骤组件库 [pd_]")
    @PreAuthorize("@ss.hasPermission('flow:step-definition:update')")
    public CommonResult<Boolean> updateStepDefinition(@Valid @RequestBody StepDefinitionSaveReqVO updateReqVO) {
        stepDefinitionService.updateStepDefinition(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除步骤定义表，可复用的步骤组件库 [pd_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:step-definition:delete')")
    public CommonResult<Boolean> deleteStepDefinition(@RequestParam("id") Long id) {
        stepDefinitionService.deleteStepDefinition(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除步骤定义表，可复用的步骤组件库 [pd_]")
                @PreAuthorize("@ss.hasPermission('flow:step-definition:delete')")
    public CommonResult<Boolean> deleteStepDefinitionList(@RequestParam("ids") List<Long> ids) {
        stepDefinitionService.deleteStepDefinitionListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得步骤定义表，可复用的步骤组件库 [pd_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:step-definition:query')")
    public CommonResult<StepDefinitionRespVO> getStepDefinition(@RequestParam("id") Long id) {
        StepDefinitionDO stepDefinition = stepDefinitionService.getStepDefinition(id);
        return success(BeanUtils.toBean(stepDefinition, StepDefinitionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得步骤定义表，可复用的步骤组件库 [pd_]分页")
    @PreAuthorize("@ss.hasPermission('flow:step-definition:query')")
    public CommonResult<PageResult<StepDefinitionRespVO>> getStepDefinitionPage(@Valid StepDefinitionPageReqVO pageReqVO) {
        PageResult<StepDefinitionDO> pageResult = stepDefinitionService.getStepDefinitionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, StepDefinitionRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出步骤定义表，可复用的步骤组件库 [pd_] Excel")
    @PreAuthorize("@ss.hasPermission('flow:step-definition:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStepDefinitionExcel(@Valid StepDefinitionPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StepDefinitionDO> list = stepDefinitionService.getStepDefinitionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "步骤定义表，可复用的步骤组件库 [pd_].xls", "数据", StepDefinitionRespVO.class,
                        BeanUtils.toBean(list, StepDefinitionRespVO.class));
    }

}