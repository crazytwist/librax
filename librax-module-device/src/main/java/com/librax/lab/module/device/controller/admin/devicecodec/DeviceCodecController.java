package com.librax.lab.module.device.controller.admin.devicecodec;

import com.librax.lab.module.device.controller.admin.devicecodec.vo.DeviceCodecPageReqVO;
import com.librax.lab.module.device.controller.admin.devicecodec.vo.DeviceCodecRespVO;
import com.librax.lab.module.device.controller.admin.devicecodec.vo.DeviceCodecSaveReqVO;
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

import com.librax.lab.module.device.dal.dataobject.devicecodec.DeviceCodecDO;
import com.librax.lab.module.device.service.devicecodec.DeviceCodecService;

@Tag(name = "管理后台 - 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]")
@RestController
@RequestMapping("/lab/device-codec")
@Validated
public class DeviceCodecController {

    @Resource
    private DeviceCodecService deviceCodecService;

    @PostMapping("/create")
    @Operation(summary = "创建设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]")
    @PreAuthorize("@ss.hasPermission('lab:device-codec:create')")
    public CommonResult<Long> createDeviceCodec(@Valid @RequestBody DeviceCodecSaveReqVO createReqVO) {
        return success(deviceCodecService.createDeviceCodec(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]")
    @PreAuthorize("@ss.hasPermission('lab:device-codec:update')")
    public CommonResult<Boolean> updateDeviceCodec(@Valid @RequestBody DeviceCodecSaveReqVO updateReqVO) {
        deviceCodecService.updateDeviceCodec(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:device-codec:delete')")
    public CommonResult<Boolean> deleteDeviceCodec(@RequestParam("id") Long id) {
        deviceCodecService.deleteDeviceCodec(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]")
                @PreAuthorize("@ss.hasPermission('lab:device-codec:delete')")
    public CommonResult<Boolean> deleteDeviceCodecList(@RequestParam("ids") List<Long> ids) {
        deviceCodecService.deleteDeviceCodecListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:device-codec:query')")
    public CommonResult<DeviceCodecRespVO> getDeviceCodec(@RequestParam("id") Long id) {
        DeviceCodecDO deviceCodec = deviceCodecService.getDeviceCodec(id);
        return success(BeanUtils.toBean(deviceCodec, DeviceCodecRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_]分页")
    @PreAuthorize("@ss.hasPermission('lab:device-codec:query')")
    public CommonResult<PageResult<DeviceCodecRespVO>> getDeviceCodecPage(@Valid DeviceCodecPageReqVO pageReqVO) {
        PageResult<DeviceCodecDO> pageResult = deviceCodecService.getDeviceCodecPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DeviceCodecRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_] Excel")
    @PreAuthorize("@ss.hasPermission('lab:device-codec:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDeviceCodecExcel(@Valid DeviceCodecPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DeviceCodecDO> list = deviceCodecService.getDeviceCodecPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_].xls", "数据", DeviceCodecRespVO.class,
                        BeanUtils.toBean(list, DeviceCodecRespVO.class));
    }

}