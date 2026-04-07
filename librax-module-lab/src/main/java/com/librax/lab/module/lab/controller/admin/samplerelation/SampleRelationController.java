package com.librax.lab.module.lab.controller.admin.samplerelation;

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

import com.librax.lab.module.lab.controller.admin.samplerelation.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleRelationDO;
import com.librax.lab.module.lab.service.samplerelation.SampleRelationService;

@Tag(name = "管理后台 - 样本谱系关系表，记录拆分/合并/分装等衍生关系")
@RestController
@RequestMapping("/lab/sample-relation")
@Validated
public class SampleRelationController {

    @Resource
    private SampleRelationService sampleRelationService;

    @PostMapping("/create")
    @Operation(summary = "创建样本谱系关系表，记录拆分/合并/分装等衍生关系")
    @PreAuthorize("@ss.hasPermission('lab:sample-relation:create')")
    public CommonResult<Long> createSampleRelation(@Valid @RequestBody SampleRelationSaveReqVO createReqVO) {
        return success(sampleRelationService.createSampleRelation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样本谱系关系表，记录拆分/合并/分装等衍生关系")
    @PreAuthorize("@ss.hasPermission('lab:sample-relation:update')")
    public CommonResult<Boolean> updateSampleRelation(@Valid @RequestBody SampleRelationSaveReqVO updateReqVO) {
        sampleRelationService.updateSampleRelation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样本谱系关系表，记录拆分/合并/分装等衍生关系")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:sample-relation:delete')")
    public CommonResult<Boolean> deleteSampleRelation(@RequestParam("id") Long id) {
        sampleRelationService.deleteSampleRelation(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除样本谱系关系表，记录拆分/合并/分装等衍生关系")
                @PreAuthorize("@ss.hasPermission('lab:sample-relation:delete')")
    public CommonResult<Boolean> deleteSampleRelationList(@RequestParam("ids") List<Long> ids) {
        sampleRelationService.deleteSampleRelationListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样本谱系关系表，记录拆分/合并/分装等衍生关系")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:sample-relation:query')")
    public CommonResult<SampleRelationRespVO> getSampleRelation(@RequestParam("id") Long id) {
        SampleRelationDO sampleRelation = sampleRelationService.getSampleRelation(id);
        return success(BeanUtils.toBean(sampleRelation, SampleRelationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样本谱系关系表，记录拆分/合并/分装等衍生关系分页")
    @PreAuthorize("@ss.hasPermission('lab:sample-relation:query')")
    public CommonResult<PageResult<SampleRelationRespVO>> getSampleRelationPage(@Valid SampleRelationPageReqVO pageReqVO) {
        PageResult<SampleRelationDO> pageResult = sampleRelationService.getSampleRelationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SampleRelationRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出样本谱系关系表，记录拆分/合并/分装等衍生关系 Excel")
    @PreAuthorize("@ss.hasPermission('lab:sample-relation:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSampleRelationExcel(@Valid SampleRelationPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SampleRelationDO> list = sampleRelationService.getSampleRelationPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "样本谱系关系表，记录拆分/合并/分装等衍生关系.xls", "数据", SampleRelationRespVO.class,
                        BeanUtils.toBean(list, SampleRelationRespVO.class));
    }

}