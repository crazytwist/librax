package com.librax.lab.module.lab.controller.admin.samplestep;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

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

import com.librax.lab.module.lab.controller.admin.samplestep.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleStepDO;
import com.librax.lab.module.lab.service.samplestep.SampleStepService;

@Tag(name = "管理后台 - 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态")
@RestController
@RequestMapping("/lab/sample-step")
@Validated
public class SampleStepController {

    @Resource
    private SampleStepService sampleStepService;

    @PostMapping("/create")
    @Operation(summary = "创建样本-步骤绑定表，记录样本在每个流程步骤中的处理状态")
    @PreAuthorize("@ss.hasPermission('lab:sample-step:create')")
    public CommonResult<Long> createSampleStep(@Valid @RequestBody SampleStepSaveReqVO createReqVO) {
        return success(sampleStepService.createSampleStep(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样本-步骤绑定表，记录样本在每个流程步骤中的处理状态")
    @PreAuthorize("@ss.hasPermission('lab:sample-step:update')")
    public CommonResult<Boolean> updateSampleStep(@Valid @RequestBody SampleStepSaveReqVO updateReqVO) {
        sampleStepService.updateSampleStep(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样本-步骤绑定表，记录样本在每个流程步骤中的处理状态")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:sample-step:delete')")
    public CommonResult<Boolean> deleteSampleStep(@RequestParam("id") Long id) {
        sampleStepService.deleteSampleStep(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除样本-步骤绑定表，记录样本在每个流程步骤中的处理状态")
                @PreAuthorize("@ss.hasPermission('lab:sample-step:delete')")
    public CommonResult<Boolean> deleteSampleStepList(@RequestParam("ids") List<Long> ids) {
        sampleStepService.deleteSampleStepListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样本-步骤绑定表，记录样本在每个流程步骤中的处理状态")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:sample-step:query')")
    public CommonResult<SampleStepRespVO> getSampleStep(@RequestParam("id") Long id) {
        SampleStepDO sampleStep = sampleStepService.getSampleStep(id);
        return success(BeanUtils.toBean(sampleStep, SampleStepRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样本-步骤绑定表，记录样本在每个流程步骤中的处理状态分页")
    @PreAuthorize("@ss.hasPermission('lab:sample-step:query')")
    public CommonResult<PageResult<SampleStepRespVO>> getSampleStepPage(@Valid SampleStepPageReqVO pageReqVO) {
        PageResult<SampleStepDO> pageResult = sampleStepService.getSampleStepPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SampleStepRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出样本-步骤绑定表，记录样本在每个流程步骤中的处理状态 Excel")
    @PreAuthorize("@ss.hasPermission('lab:sample-step:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSampleStepExcel(@Valid SampleStepPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SampleStepDO> list = sampleStepService.getSampleStepPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "样本-步骤绑定表，记录样本在每个流程步骤中的处理状态.xls", "数据", SampleStepRespVO.class,
                        BeanUtils.toBean(list, SampleStepRespVO.class));
    }

}