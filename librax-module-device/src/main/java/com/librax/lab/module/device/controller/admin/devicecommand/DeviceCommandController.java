package com.librax.lab.module.device.controller.admin.devicecommand;

import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandPageReqVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandRespVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandSaveReqVO;
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

import com.librax.lab.module.lab.controller.admin.devicecommand.vo.*;
import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.service.devicecommand.DeviceCommandService;

@Tag(name = "管理后台 - 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]")
@RestController
@RequestMapping("/lab/device-command")
@Validated
public class DeviceCommandController {

    @Resource
    private DeviceCommandService deviceCommandService;

    @PostMapping("/create")
    @Operation(summary = "创建设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]")
    @PreAuthorize("@ss.hasPermission('lab:device-command:create')")
    public CommonResult<Long> createDeviceCommand(@Valid @RequestBody DeviceCommandSaveReqVO createReqVO) {
        return success(deviceCommandService.createDeviceCommand(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]")
    @PreAuthorize("@ss.hasPermission('lab:device-command:update')")
    public CommonResult<Boolean> updateDeviceCommand(@Valid @RequestBody DeviceCommandSaveReqVO updateReqVO) {
        deviceCommandService.updateDeviceCommand(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:device-command:delete')")
    public CommonResult<Boolean> deleteDeviceCommand(@RequestParam("id") Long id) {
        deviceCommandService.deleteDeviceCommand(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]")
                @PreAuthorize("@ss.hasPermission('lab:device-command:delete')")
    public CommonResult<Boolean> deleteDeviceCommandList(@RequestParam("ids") List<Long> ids) {
        deviceCommandService.deleteDeviceCommandListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:device-command:query')")
    public CommonResult<DeviceCommandRespVO> getDeviceCommand(@RequestParam("id") Long id) {
        DeviceCommandDO deviceCommand = deviceCommandService.getDeviceCommand(id);
        return success(BeanUtils.toBean(deviceCommand, DeviceCommandRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]分页")
    @PreAuthorize("@ss.hasPermission('lab:device-command:query')")
    public CommonResult<PageResult<DeviceCommandRespVO>> getDeviceCommandPage(@Valid DeviceCommandPageReqVO pageReqVO) {
        PageResult<DeviceCommandDO> pageResult = deviceCommandService.getDeviceCommandPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DeviceCommandRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_] Excel")
    @PreAuthorize("@ss.hasPermission('lab:device-command:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDeviceCommandExcel(@Valid DeviceCommandPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DeviceCommandDO> list = deviceCommandService.getDeviceCommandPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_].xls", "数据", DeviceCommandRespVO.class,
                        BeanUtils.toBean(list, DeviceCommandRespVO.class));
    }

}