package com.librax.lab.module.resource.controller.admin.materialconfig;

import com.librax.lab.framework.apilog.core.annotation.ApiAccessLog;
import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.framework.excel.core.util.ExcelUtils;
import com.librax.lab.module.resource.controller.admin.materialconfig.vo.MaterialConfigPageReqVO;
import com.librax.lab.module.resource.controller.admin.materialconfig.vo.MaterialConfigRespVO;
import com.librax.lab.module.resource.controller.admin.materialconfig.vo.MaterialConfigSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.materialconfig.MaterialConfigDO;
import com.librax.lab.module.resource.service.materialconfig.MaterialConfigService;
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


@Tag(name = "管理后台 - 物料配置")
@RestController
@RequestMapping("/resource/material-config")
@Validated
public class MaterialConfigController {

    @Resource
    private MaterialConfigService materialConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建物料配置")
    @PreAuthorize("@ss.hasPermission('res:material-config:create')")
    public CommonResult<Long> createMaterialConfig(@Valid @RequestBody MaterialConfigSaveReqVO createReqVO) {
        return success(materialConfigService.createMaterialConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新物料配置")
    @PreAuthorize("@ss.hasPermission('res:material-config:update')")
    public CommonResult<Boolean> updateMaterialConfig(@Valid @RequestBody MaterialConfigSaveReqVO updateReqVO) {
        materialConfigService.updateMaterialConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物料配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('res:material-config:delete')")
    public CommonResult<Boolean> deleteMaterialConfig(@RequestParam("id") Long id) {
        materialConfigService.deleteMaterialConfig(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除物料配置")
    @PreAuthorize("@ss.hasPermission('res:material-config:delete')")
    public CommonResult<Boolean> deleteMaterialConfigList(@RequestParam("ids") List<Long> ids) {
        materialConfigService.deleteMaterialConfigListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得物料配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('res:material-config:query')")
    public CommonResult<MaterialConfigRespVO> getMaterialConfig(@RequestParam("id") Long id) {
        MaterialConfigDO materialConfig = materialConfigService.getMaterialConfig(id);
        return success(BeanUtils.toBean(materialConfig, MaterialConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得物料配置分页")
    @PreAuthorize("@ss.hasPermission('res:material-config:query')")
    public CommonResult<PageResult<MaterialConfigRespVO>> getMaterialConfigPage(@Valid MaterialConfigPageReqVO pageReqVO) {
        PageResult<MaterialConfigDO> pageResult = materialConfigService.getMaterialConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MaterialConfigRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出物料配置 Excel")
    @PreAuthorize("@ss.hasPermission('res:material-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMaterialConfigExcel(@Valid MaterialConfigPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MaterialConfigDO> list = materialConfigService.getMaterialConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "物料配置.xls", "数据", MaterialConfigRespVO.class,
                BeanUtils.toBean(list, MaterialConfigRespVO.class));
    }

}