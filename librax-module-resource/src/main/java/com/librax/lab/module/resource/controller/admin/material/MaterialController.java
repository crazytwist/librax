package com.librax.lab.module.resource.controller.admin.material;

import com.librax.lab.framework.apilog.core.annotation.ApiAccessLog;
import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.framework.excel.core.util.ExcelUtils;
import com.librax.lab.module.resource.controller.admin.material.vo.MaterialPageReqVO;
import com.librax.lab.module.resource.controller.admin.material.vo.MaterialRespVO;
import com.librax.lab.module.resource.controller.admin.material.vo.MaterialSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.material.MaterialDO;
import com.librax.lab.module.resource.service.material.MaterialService;
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

import static com.librax.lab.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.librax.lab.framework.common.pojo.CommonResult.success;


@Tag(name = "管理后台 - 物料基础信息")
@RestController
@RequestMapping("/resource/material")
@Validated
public class MaterialController {

    @Resource
    private MaterialService materialService;

    @PostMapping("/create")
    @Operation(summary = "创建物料基础信息")
    @PreAuthorize("@ss.hasPermission('res:material:create')")
    public CommonResult<Long> createMaterial(@Valid @RequestBody MaterialSaveReqVO createReqVO) {
        return success(materialService.createMaterial(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新物料基础信息")
    @PreAuthorize("@ss.hasPermission('res:material:update')")
    public CommonResult<Boolean> updateMaterial(@Valid @RequestBody MaterialSaveReqVO updateReqVO) {
        materialService.updateMaterial(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物料基础信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('res:material:delete')")
    public CommonResult<Boolean> deleteMaterial(@RequestParam("id") Long id) {
        materialService.deleteMaterial(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除物料基础信息")
                @PreAuthorize("@ss.hasPermission('res:material:delete')")
    public CommonResult<Boolean> deleteMaterialList(@RequestParam("ids") List<Long> ids) {
        materialService.deleteMaterialListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得物料基础信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('res:material:query')")
    public CommonResult<MaterialRespVO> getMaterial(@RequestParam("id") Long id) {
        MaterialDO material = materialService.getMaterial(id);
        return success(BeanUtils.toBean(material, MaterialRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得物料基础信息分页")
    @PreAuthorize("@ss.hasPermission('res:material:query')")
    public CommonResult<PageResult<MaterialRespVO>> getMaterialPage(@Valid MaterialPageReqVO pageReqVO) {
        PageResult<MaterialDO> pageResult = materialService.getMaterialPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MaterialRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出物料基础信息 Excel")
    @PreAuthorize("@ss.hasPermission('res:material:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMaterialExcel(@Valid MaterialPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MaterialDO> list = materialService.getMaterialPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "物料基础信息.xls", "数据", MaterialRespVO.class,
                        BeanUtils.toBean(list, MaterialRespVO.class));
    }

}