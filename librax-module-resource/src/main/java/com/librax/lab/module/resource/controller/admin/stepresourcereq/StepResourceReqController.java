package com.librax.lab.module.resource.controller.admin.stepresourcereq;

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

import com.librax.lab.module.resource.controller.admin.stepresourcereq.vo.*;
import com.librax.lab.module.resource.dal.dataobject.stepresourcereq.StepResourceReqDO;
import com.librax.lab.module.resource.service.stepresourcereq.StepResourceReqService;

@Tag(name = "管理后台 - 步骤资源需求定义，一个步骤节点可配多行（一步多资源）")
@RestController
@RequestMapping("/resource/step-resource-req")
@Validated
public class StepResourceReqController {

    @Resource
    private StepResourceReqService stepResourceReqService;

    @PostMapping("/create")
    @Operation(summary = "创建步骤资源需求定义，一个步骤节点可配多行（一步多资源）")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-req:create')")
    public CommonResult<Long> createStepResourceReq(@Valid @RequestBody StepResourceReqSaveReqVO createReqVO) {
        return success(stepResourceReqService.createStepResourceReq(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新步骤资源需求定义，一个步骤节点可配多行（一步多资源）")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-req:update')")
    public CommonResult<Boolean> updateStepResourceReq(@Valid @RequestBody StepResourceReqSaveReqVO updateReqVO) {
        stepResourceReqService.updateStepResourceReq(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除步骤资源需求定义，一个步骤节点可配多行（一步多资源）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('resource:step-resource-req:delete')")
    public CommonResult<Boolean> deleteStepResourceReq(@RequestParam("id") Long id) {
        stepResourceReqService.deleteStepResourceReq(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除步骤资源需求定义，一个步骤节点可配多行（一步多资源）")
                @PreAuthorize("@ss.hasPermission('resource:step-resource-req:delete')")
    public CommonResult<Boolean> deleteStepResourceReqList(@RequestParam("ids") List<Long> ids) {
        stepResourceReqService.deleteStepResourceReqListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得步骤资源需求定义，一个步骤节点可配多行（一步多资源）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-req:query')")
    public CommonResult<StepResourceReqRespVO> getStepResourceReq(@RequestParam("id") Long id) {
        StepResourceReqDO stepResourceReq = stepResourceReqService.getStepResourceReq(id);
        return success(BeanUtils.toBean(stepResourceReq, StepResourceReqRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得步骤资源需求定义，一个步骤节点可配多行（一步多资源）分页")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-req:query')")
    public CommonResult<PageResult<StepResourceReqRespVO>> getStepResourceReqPage(@Valid StepResourceReqPageReqVO pageReqVO) {
        PageResult<StepResourceReqDO> pageResult = stepResourceReqService.getStepResourceReqPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, StepResourceReqRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出步骤资源需求定义，一个步骤节点可配多行（一步多资源） Excel")
    @PreAuthorize("@ss.hasPermission('resource:step-resource-req:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStepResourceReqExcel(@Valid StepResourceReqPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StepResourceReqDO> list = stepResourceReqService.getStepResourceReqPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "步骤资源需求定义，一个步骤节点可配多行（一步多资源）.xls", "数据", StepResourceReqRespVO.class,
                        BeanUtils.toBean(list, StepResourceReqRespVO.class));
    }

}