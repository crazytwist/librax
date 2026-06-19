package com.librax.lab.module.device.controller.admin.devicecommand;

import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandExecuteReqVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandExecuteRespVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandPageReqVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandRespVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandSaveReqVO;
import com.librax.lab.module.device.service.devicedirectexec.DeviceDirectExecService;
import jakarta.annotation.security.PermitAll;
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

import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.service.devicecommand.DeviceCommandService;

@Tag(name = "管理后台 - 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]")
@RestController
@RequestMapping("/lab/device-command")
@Validated
public class DeviceCommandController {

    @Resource
    private DeviceCommandService deviceCommandService;

    @Resource
    private DeviceDirectExecService deviceDirectExecService;

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

    // ----------------------------------------------------------------
    //  设备指令直接执行
    // ----------------------------------------------------------------

    @PostMapping("/execute")
    @Operation(summary = "直接执行设备指令",
            description = "不依赖流水线，直接触发设备执行指定指令。" +
                    "接口立即返回 execId，设备完成后可通过 /execute/result 轮询结果。" +
                    "适用于设备联调、运维操作、人工干预等场景。")
    @PermitAll
    @ApiAccessLog(operateType = OTHER)
    public CommonResult<DeviceCommandExecuteRespVO> executeDeviceCommand(
            @Valid @RequestBody DeviceCommandExecuteReqVO reqVO) {
        return success(deviceDirectExecService.execute(reqVO));
    }

    @GetMapping("/execute/result")
    @Operation(summary = "查询设备指令执行结果",
            description = "通过 execute 接口返回的 execId 轮询执行状态与结果。" +
                    "执行记录最长保留 2 小时，超时后返回 NOT_FOUND。")
    @Parameter(name = "execId", description = "执行ID（由 /execute 接口返回）", required = true, example = "de-a1b2c3d4")
    @PreAuthorize("@ss.hasPermission('lab:device-command:execute')")
    public CommonResult<DeviceCommandExecuteRespVO> getDeviceCommandExecuteResult(
            @RequestParam("execId") String execId) {
        return success(deviceDirectExecService.getResult(execId));
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