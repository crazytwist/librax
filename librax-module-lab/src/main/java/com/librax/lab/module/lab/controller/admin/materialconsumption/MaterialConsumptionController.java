package com.librax.lab.module.lab.controller.admin.materialconsumption;

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

import com.librax.lab.module.lab.controller.admin.materialconsumption.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialconsumption.MaterialConsumptionDO;
import com.librax.lab.module.lab.service.materialconsumption.MaterialConsumptionService;

@Tag(name = "管理后台 - 步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理")
@RestController
@RequestMapping("/lab/material-consumption")
@Validated
public class MaterialConsumptionController {

    @Resource
    private MaterialConsumptionService materialConsumptionService;

    @PostMapping("/create")
    @Operation(summary = "创建步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:material-consumption:create')")
    public CommonResult<Long> createMaterialConsumption(@Valid @RequestBody MaterialConsumptionSaveReqVO createReqVO) {
        return success(materialConsumptionService.createMaterialConsumption(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:material-consumption:update')")
    public CommonResult<Boolean> updateMaterialConsumption(@Valid @RequestBody MaterialConsumptionSaveReqVO updateReqVO) {
        materialConsumptionService.updateMaterialConsumption(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:material-consumption:delete')")
    public CommonResult<Boolean> deleteMaterialConsumption(@RequestParam("id") Long id) {
        materialConsumptionService.deleteMaterialConsumption(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理")
                @PreAuthorize("@ss.hasPermission('lab:material-consumption:delete')")
    public CommonResult<Boolean> deleteMaterialConsumptionList(@RequestParam("ids") List<Long> ids) {
        materialConsumptionService.deleteMaterialConsumptionListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:material-consumption:query')")
    public CommonResult<MaterialConsumptionRespVO> getMaterialConsumption(@RequestParam("id") Long id) {
        MaterialConsumptionDO materialConsumption = materialConsumptionService.getMaterialConsumption(id);
        return success(BeanUtils.toBean(materialConsumption, MaterialConsumptionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理分页")
    @PreAuthorize("@ss.hasPermission('lab:material-consumption:query')")
    public CommonResult<PageResult<MaterialConsumptionRespVO>> getMaterialConsumptionPage(@Valid MaterialConsumptionPageReqVO pageReqVO) {
        PageResult<MaterialConsumptionDO> pageResult = materialConsumptionService.getMaterialConsumptionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MaterialConsumptionRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理 Excel")
    @PreAuthorize("@ss.hasPermission('lab:material-consumption:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMaterialConsumptionExcel(@Valid MaterialConsumptionPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MaterialConsumptionDO> list = materialConsumptionService.getMaterialConsumptionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理.xls", "数据", MaterialConsumptionRespVO.class,
                        BeanUtils.toBean(list, MaterialConsumptionRespVO.class));
    }

}