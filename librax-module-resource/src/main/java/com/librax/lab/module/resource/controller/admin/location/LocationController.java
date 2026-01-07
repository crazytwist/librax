package com.librax.lab.module.resource.controller.admin.location;

import com.librax.lab.framework.apilog.core.annotation.ApiAccessLog;
import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.framework.excel.core.util.ExcelUtils;
import com.librax.lab.module.resource.controller.admin.location.vo.LocationPageReqVO;
import com.librax.lab.module.resource.controller.admin.location.vo.LocationRespVO;
import com.librax.lab.module.resource.controller.admin.location.vo.LocationSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.location.LocationDO;
import com.librax.lab.module.resource.service.location.LocationService;
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

import static com.librax.lab.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.librax.lab.framework.common.pojo.CommonResult.success;


@Tag(name = "管理后台 - 区位信息")
@RestController
@RequestMapping("/resource/location")
@Validated
public class LocationController {

    @Resource
    private LocationService locationService;

    @PostMapping("/create")
    @Operation(summary = "创建区位信息")
    @PreAuthorize("@ss.hasPermission('res:location:create')")
    public CommonResult<Long> createLocation(@Valid @RequestBody LocationSaveReqVO createReqVO) {
        return success(locationService.createLocation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新区位信息")
    @PreAuthorize("@ss.hasPermission('res:location:update')")
    public CommonResult<Boolean> updateLocation(@Valid @RequestBody LocationSaveReqVO updateReqVO) {
        locationService.updateLocation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除区位信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('res:location:delete')")
    public CommonResult<Boolean> deleteLocation(@RequestParam("id") Long id) {
        locationService.deleteLocation(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除区位信息")
                @PreAuthorize("@ss.hasPermission('res:location:delete')")
    public CommonResult<Boolean> deleteLocationList(@RequestParam("ids") List<Long> ids) {
        locationService.deleteLocationListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得区位信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('res:location:query')")
    public CommonResult<LocationRespVO> getLocation(@RequestParam("id") Long id) {
        LocationDO location = locationService.getLocation(id);
        return success(BeanUtils.toBean(location, LocationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得区位信息分页")
    @PreAuthorize("@ss.hasPermission('res:location:query')")
    public CommonResult<PageResult<LocationRespVO>> getLocationPage(@Valid LocationPageReqVO pageReqVO) {
        PageResult<LocationDO> pageResult = locationService.getLocationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, LocationRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出区位信息 Excel")
    @PreAuthorize("@ss.hasPermission('res:location:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLocationExcel(@Valid LocationPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LocationDO> list = locationService.getLocationPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "区位信息.xls", "数据", LocationRespVO.class,
                        BeanUtils.toBean(list, LocationRespVO.class));
    }

}