package com.librax.lab.module.lab.controller.admin.materialinstance;

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

import com.librax.lab.module.lab.controller.admin.materialinstance.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.module.lab.service.materialinstance.MaterialInstanceService;

@Tag(name = "管理后台 - 物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理")
@RestController
@RequestMapping("/lab/material-instance")
@Validated
public class MaterialInstanceController {

    @Resource
    private MaterialInstanceService materialInstanceService;

    @PostMapping("/create")
    @Operation(summary = "创建物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:material-instance:create')")
    public CommonResult<Long> createMaterialInstance(@Valid @RequestBody MaterialInstanceSaveReqVO createReqVO) {
        return success(materialInstanceService.createMaterialInstance(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:material-instance:update')")
    public CommonResult<Boolean> updateMaterialInstance(@Valid @RequestBody MaterialInstanceSaveReqVO updateReqVO) {
        materialInstanceService.updateMaterialInstance(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:material-instance:delete')")
    public CommonResult<Boolean> deleteMaterialInstance(@RequestParam("id") Long id) {
        materialInstanceService.deleteMaterialInstance(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理")
                @PreAuthorize("@ss.hasPermission('lab:material-instance:delete')")
    public CommonResult<Boolean> deleteMaterialInstanceList(@RequestParam("ids") List<Long> ids) {
        materialInstanceService.deleteMaterialInstanceListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:material-instance:query')")
    public CommonResult<MaterialInstanceRespVO> getMaterialInstance(@RequestParam("id") Long id) {
        MaterialInstanceDO materialInstance = materialInstanceService.getMaterialInstance(id);
        return success(BeanUtils.toBean(materialInstance, MaterialInstanceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理分页")
    @PreAuthorize("@ss.hasPermission('lab:material-instance:query')")
    public CommonResult<PageResult<MaterialInstanceRespVO>> getMaterialInstancePage(@Valid MaterialInstancePageReqVO pageReqVO) {
        PageResult<MaterialInstanceDO> pageResult = materialInstanceService.getMaterialInstancePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MaterialInstanceRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理 Excel")
    @PreAuthorize("@ss.hasPermission('lab:material-instance:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMaterialInstanceExcel(@Valid MaterialInstancePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MaterialInstanceDO> list = materialInstanceService.getMaterialInstancePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理.xls", "数据", MaterialInstanceRespVO.class,
                        BeanUtils.toBean(list, MaterialInstanceRespVO.class));
    }

}