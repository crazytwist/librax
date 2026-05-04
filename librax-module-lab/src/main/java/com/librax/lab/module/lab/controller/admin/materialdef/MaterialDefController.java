package com.librax.lab.module.lab.controller.admin.materialdef;

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

import com.librax.lab.module.lab.controller.admin.materialdef.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialdef.MaterialDefDO;
import com.librax.lab.module.lab.service.materialdef.MaterialDefService;

@Tag(name = "管理后台 - 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理")
@RestController
@RequestMapping("/lab/material-def")
@Validated
public class MaterialDefController {

    @Resource
    private MaterialDefService materialDefService;

    @PostMapping("/create")
    @Operation(summary = "创建内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:material-def:create')")
    public CommonResult<Long> createMaterialDef(@Valid @RequestBody MaterialDefSaveReqVO createReqVO) {
        return success(materialDefService.createMaterialDef(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:material-def:update')")
    public CommonResult<Boolean> updateMaterialDef(@Valid @RequestBody MaterialDefSaveReqVO updateReqVO) {
        materialDefService.updateMaterialDef(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:material-def:delete')")
    public CommonResult<Boolean> deleteMaterialDef(@RequestParam("id") Long id) {
        materialDefService.deleteMaterialDef(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理")
                @PreAuthorize("@ss.hasPermission('lab:material-def:delete')")
    public CommonResult<Boolean> deleteMaterialDefList(@RequestParam("ids") List<Long> ids) {
        materialDefService.deleteMaterialDefListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:material-def:query')")
    public CommonResult<MaterialDefRespVO> getMaterialDef(@RequestParam("id") Long id) {
        MaterialDefDO materialDef = materialDefService.getMaterialDef(id);
        return success(BeanUtils.toBean(materialDef, MaterialDefRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理分页")
    @PreAuthorize("@ss.hasPermission('lab:material-def:query')")
    public CommonResult<PageResult<MaterialDefRespVO>> getMaterialDefPage(@Valid MaterialDefPageReqVO pageReqVO) {
        PageResult<MaterialDefDO> pageResult = materialDefService.getMaterialDefPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MaterialDefRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理 Excel")
    @PreAuthorize("@ss.hasPermission('lab:material-def:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMaterialDefExcel(@Valid MaterialDefPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MaterialDefDO> list = materialDefService.getMaterialDefPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理.xls", "数据", MaterialDefRespVO.class,
                        BeanUtils.toBean(list, MaterialDefRespVO.class));
    }

}