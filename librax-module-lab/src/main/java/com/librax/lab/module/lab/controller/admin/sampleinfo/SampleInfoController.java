package com.librax.lab.module.lab.controller.admin.sampleinfo;

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

import com.librax.lab.module.lab.controller.admin.sampleinfo.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleInfoDO;
import com.librax.lab.module.lab.service.sampleinfo.SampleInfoService;

@Tag(name = "管理后台 - 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联")
@RestController
@RequestMapping("/lab/sample-info")
@Validated
public class SampleInfoController {

    @Resource
    private SampleInfoService sampleInfoService;

    @PostMapping("/create")
    @Operation(summary = "创建样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联")
    @PreAuthorize("@ss.hasPermission('lab:sample-info:create')")
    public CommonResult<Long> createSampleInfo(@Valid @RequestBody SampleInfoSaveReqVO createReqVO) {
        return success(sampleInfoService.createSampleInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联")
    @PreAuthorize("@ss.hasPermission('lab:sample-info:update')")
    public CommonResult<Boolean> updateSampleInfo(@Valid @RequestBody SampleInfoSaveReqVO updateReqVO) {
        sampleInfoService.updateSampleInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:sample-info:delete')")
    public CommonResult<Boolean> deleteSampleInfo(@RequestParam("id") Long id) {
        sampleInfoService.deleteSampleInfo(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联")
                @PreAuthorize("@ss.hasPermission('lab:sample-info:delete')")
    public CommonResult<Boolean> deleteSampleInfoList(@RequestParam("ids") List<Long> ids) {
        sampleInfoService.deleteSampleInfoListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:sample-info:query')")
    public CommonResult<SampleInfoRespVO> getSampleInfo(@RequestParam("id") Long id) {
        SampleInfoDO sampleInfo = sampleInfoService.getSampleInfo(id);
        return success(BeanUtils.toBean(sampleInfo, SampleInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联分页")
    @PreAuthorize("@ss.hasPermission('lab:sample-info:query')")
    public CommonResult<PageResult<SampleInfoRespVO>> getSampleInfoPage(@Valid SampleInfoPageReqVO pageReqVO) {
        PageResult<SampleInfoDO> pageResult = sampleInfoService.getSampleInfoPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SampleInfoRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联 Excel")
    @PreAuthorize("@ss.hasPermission('lab:sample-info:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSampleInfoExcel(@Valid SampleInfoPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SampleInfoDO> list = sampleInfoService.getSampleInfoPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联.xls", "数据", SampleInfoRespVO.class,
                        BeanUtils.toBean(list, SampleInfoRespVO.class));
    }

}