package com.librax.lab.module.resource.controller.admin.zonequota;

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

import com.librax.lab.module.resource.controller.admin.zonequota.vo.*;
import com.librax.lab.module.resource.dal.dataobject.zonequota.ZoneQuotaDO;
import com.librax.lab.module.resource.service.zonequota.ZoneQuotaService;

@Tag(name = "管理后台 - 区域对共享资源的配额")
@RestController
@RequestMapping("/resource/zone-quota")
@Validated
public class ZoneQuotaController {

    @Resource
    private ZoneQuotaService zoneQuotaService;

    @PostMapping("/create")
    @Operation(summary = "创建区域对共享资源的配额")
    @PreAuthorize("@ss.hasPermission('resource:zone-quota:create')")
    public CommonResult<Long> createZoneQuota(@Valid @RequestBody ZoneQuotaSaveReqVO createReqVO) {
        return success(zoneQuotaService.createZoneQuota(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新区域对共享资源的配额")
    @PreAuthorize("@ss.hasPermission('resource:zone-quota:update')")
    public CommonResult<Boolean> updateZoneQuota(@Valid @RequestBody ZoneQuotaSaveReqVO updateReqVO) {
        zoneQuotaService.updateZoneQuota(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除区域对共享资源的配额")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('resource:zone-quota:delete')")
    public CommonResult<Boolean> deleteZoneQuota(@RequestParam("id") Long id) {
        zoneQuotaService.deleteZoneQuota(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除区域对共享资源的配额")
                @PreAuthorize("@ss.hasPermission('resource:zone-quota:delete')")
    public CommonResult<Boolean> deleteZoneQuotaList(@RequestParam("ids") List<Long> ids) {
        zoneQuotaService.deleteZoneQuotaListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得区域对共享资源的配额")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('resource:zone-quota:query')")
    public CommonResult<ZoneQuotaRespVO> getZoneQuota(@RequestParam("id") Long id) {
        ZoneQuotaDO zoneQuota = zoneQuotaService.getZoneQuota(id);
        return success(BeanUtils.toBean(zoneQuota, ZoneQuotaRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得区域对共享资源的配额分页")
    @PreAuthorize("@ss.hasPermission('resource:zone-quota:query')")
    public CommonResult<PageResult<ZoneQuotaRespVO>> getZoneQuotaPage(@Valid ZoneQuotaPageReqVO pageReqVO) {
        PageResult<ZoneQuotaDO> pageResult = zoneQuotaService.getZoneQuotaPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ZoneQuotaRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出区域对共享资源的配额 Excel")
    @PreAuthorize("@ss.hasPermission('resource:zone-quota:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportZoneQuotaExcel(@Valid ZoneQuotaPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ZoneQuotaDO> list = zoneQuotaService.getZoneQuotaPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "区域对共享资源的配额.xls", "数据", ZoneQuotaRespVO.class,
                        BeanUtils.toBean(list, ZoneQuotaRespVO.class));
    }

}