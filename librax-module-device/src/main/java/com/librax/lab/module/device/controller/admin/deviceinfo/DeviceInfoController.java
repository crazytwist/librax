package com.librax.lab.module.device.controller.admin.deviceinfo;

import com.librax.lab.module.device.controller.admin.deviceinfo.vo.DeviceInfoPageReqVO;
import com.librax.lab.module.device.controller.admin.deviceinfo.vo.DeviceInfoRespVO;
import com.librax.lab.module.device.controller.admin.deviceinfo.vo.DeviceInfoSaveReqVO;
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

import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import com.librax.lab.module.device.service.deviceinfo.DeviceInfoService;

@Tag(name = "管理后台 - 设备基本信息表，一行一台物理设备 [lab_device_]")
@RestController
@RequestMapping("/lab/device-info")
@Validated
public class DeviceInfoController {

    @Resource
    private DeviceInfoService deviceInfoService;

    @PostMapping("/create")
    @Operation(summary = "创建设备基本信息表，一行一台物理设备 [lab_device_]")
    @PreAuthorize("@ss.hasPermission('lab:device-info:create')")
    public CommonResult<Long> createDeviceInfo(@Valid @RequestBody DeviceInfoSaveReqVO createReqVO) {
        return success(deviceInfoService.createDeviceInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备基本信息表，一行一台物理设备 [lab_device_]")
    @PreAuthorize("@ss.hasPermission('lab:device-info:update')")
    public CommonResult<Boolean> updateDeviceInfo(@Valid @RequestBody DeviceInfoSaveReqVO updateReqVO) {
        deviceInfoService.updateDeviceInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备基本信息表，一行一台物理设备 [lab_device_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:device-info:delete')")
    public CommonResult<Boolean> deleteDeviceInfo(@RequestParam("id") Long id) {
        deviceInfoService.deleteDeviceInfo(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除设备基本信息表，一行一台物理设备 [lab_device_]")
                @PreAuthorize("@ss.hasPermission('lab:device-info:delete')")
    public CommonResult<Boolean> deleteDeviceInfoList(@RequestParam("ids") List<Long> ids) {
        deviceInfoService.deleteDeviceInfoListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备基本信息表，一行一台物理设备 [lab_device_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:device-info:query')")
    public CommonResult<DeviceInfoRespVO> getDeviceInfo(@RequestParam("id") Long id) {
        DeviceInfoDO deviceInfo = deviceInfoService.getDeviceInfo(id);
        return success(BeanUtils.toBean(deviceInfo, DeviceInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备基本信息表，一行一台物理设备 [lab_device_]分页")
    @PreAuthorize("@ss.hasPermission('lab:device-info:query')")
    public CommonResult<PageResult<DeviceInfoRespVO>> getDeviceInfoPage(@Valid DeviceInfoPageReqVO pageReqVO) {
        PageResult<DeviceInfoDO> pageResult = deviceInfoService.getDeviceInfoPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DeviceInfoRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备基本信息表，一行一台物理设备 [lab_device_] Excel")
    @PreAuthorize("@ss.hasPermission('lab:device-info:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDeviceInfoExcel(@Valid DeviceInfoPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DeviceInfoDO> list = deviceInfoService.getDeviceInfoPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "设备基本信息表，一行一台物理设备 [lab_device_].xls", "数据", DeviceInfoRespVO.class,
                        BeanUtils.toBean(list, DeviceInfoRespVO.class));
    }

}