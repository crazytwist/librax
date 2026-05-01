package com.librax.lab.module.resource.controller.admin.stepresourcehold;

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

import com.librax.lab.module.resource.controller.admin.stepresourcehold.vo.*;
import com.librax.lab.module.resource.dal.dataobject.stepresourcehold.StepResourceHoldDO;
import com.librax.lab.module.resource.service.stepresourcehold.StepResourceHoldService;

@Tag(name = "管理后台 - 步骤执行资源占用记录，released_at IS NULL 表示当前持有中")
@RestController
@RequestMapping("/resource/step-resource-hold")
@Validated
public class StepResourceHoldController {

    @Resource
    private StepResourceHoldService stepResourceHoldService;

    @PostMapping("/create")
    @Operation(summary = "创建步骤执行资源占用记录，released_at IS NULL 表示当前持有中")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-hold:create')")
    public CommonResult<Long> createStepResourceHold(@Valid @RequestBody StepResourceHoldSaveReqVO createReqVO) {
        return success(stepResourceHoldService.createStepResourceHold(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新步骤执行资源占用记录，released_at IS NULL 表示当前持有中")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-hold:update')")
    public CommonResult<Boolean> updateStepResourceHold(@Valid @RequestBody StepResourceHoldSaveReqVO updateReqVO) {
        stepResourceHoldService.updateStepResourceHold(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除步骤执行资源占用记录，released_at IS NULL 表示当前持有中")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('resource:step-resource-hold:delete')")
    public CommonResult<Boolean> deleteStepResourceHold(@RequestParam("id") Long id) {
        stepResourceHoldService.deleteStepResourceHold(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除步骤执行资源占用记录，released_at IS NULL 表示当前持有中")
                @PreAuthorize("@ss.hasPermission('resource:step-resource-hold:delete')")
    public CommonResult<Boolean> deleteStepResourceHoldList(@RequestParam("ids") List<Long> ids) {
        stepResourceHoldService.deleteStepResourceHoldListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得步骤执行资源占用记录，released_at IS NULL 表示当前持有中")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-hold:query')")
    public CommonResult<StepResourceHoldRespVO> getStepResourceHold(@RequestParam("id") Long id) {
        StepResourceHoldDO stepResourceHold = stepResourceHoldService.getStepResourceHold(id);
        return success(BeanUtils.toBean(stepResourceHold, StepResourceHoldRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得步骤执行资源占用记录，released_at IS NULL 表示当前持有中分页")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-hold:query')")
    public CommonResult<PageResult<StepResourceHoldRespVO>> getStepResourceHoldPage(@Valid StepResourceHoldPageReqVO pageReqVO) {
        PageResult<StepResourceHoldDO> pageResult = stepResourceHoldService.getStepResourceHoldPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, StepResourceHoldRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出步骤执行资源占用记录，released_at IS NULL 表示当前持有中 Excel")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-hold:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStepResourceHoldExcel(@Valid StepResourceHoldPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StepResourceHoldDO> list = stepResourceHoldService.getStepResourceHoldPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "步骤执行资源占用记录，released_at IS NULL 表示当前持有中.xls", "数据", StepResourceHoldRespVO.class,
                        BeanUtils.toBean(list, StepResourceHoldRespVO.class));
    }

}