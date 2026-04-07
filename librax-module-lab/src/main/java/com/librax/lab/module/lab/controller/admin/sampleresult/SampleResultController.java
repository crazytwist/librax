package com.librax.lab.module.lab.controller.admin.sampleresult;

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

import com.librax.lab.module.lab.controller.admin.sampleresult.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleResultDO;
import com.librax.lab.module.lab.service.sampleresult.SampleResultService;

@Tag(name = "管理后台 - 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]")
@RestController
@RequestMapping("/lab/sample-result")
@Validated
public class SampleResultController {

    @Resource
    private SampleResultService sampleResultService;

    @PostMapping("/create")
    @Operation(summary = "创建样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]")
    @PreAuthorize("@ss.hasPermission('lab:sample-result:create')")
    public CommonResult<Long> createSampleResult(@Valid @RequestBody SampleResultSaveReqVO createReqVO) {
        return success(sampleResultService.createSampleResult(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]")
    @PreAuthorize("@ss.hasPermission('lab:sample-result:update')")
    public CommonResult<Boolean> updateSampleResult(@Valid @RequestBody SampleResultSaveReqVO updateReqVO) {
        sampleResultService.updateSampleResult(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:sample-result:delete')")
    public CommonResult<Boolean> deleteSampleResult(@RequestParam("id") Long id) {
        sampleResultService.deleteSampleResult(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]")
                @PreAuthorize("@ss.hasPermission('lab:sample-result:delete')")
    public CommonResult<Boolean> deleteSampleResultList(@RequestParam("ids") List<Long> ids) {
        sampleResultService.deleteSampleResultListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:sample-result:query')")
    public CommonResult<SampleResultRespVO> getSampleResult(@RequestParam("id") Long id) {
        SampleResultDO sampleResult = sampleResultService.getSampleResult(id);
        return success(BeanUtils.toBean(sampleResult, SampleResultRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]分页")
    @PreAuthorize("@ss.hasPermission('lab:sample-result:query')")
    public CommonResult<PageResult<SampleResultRespVO>> getSampleResultPage(@Valid SampleResultPageReqVO pageReqVO) {
        PageResult<SampleResultDO> pageResult = sampleResultService.getSampleResultPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SampleResultRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_] Excel")
    @PreAuthorize("@ss.hasPermission('lab:sample-result:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSampleResultExcel(@Valid SampleResultPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SampleResultDO> list = sampleResultService.getSampleResultPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_].xls", "数据", SampleResultRespVO.class,
                        BeanUtils.toBean(list, SampleResultRespVO.class));
    }

}