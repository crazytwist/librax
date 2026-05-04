package com.librax.lab.module.resource.controller.admin.slotinfo;

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

import com.librax.lab.module.resource.controller.admin.slotinfo.vo.*;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.service.slotinfo.SlotInfoService;

@Tag(name = "管理后台 - 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理")
@RestController
@RequestMapping("/resource/slot-info")
@Validated
public class SlotInfoController {

    @Resource
    private SlotInfoService slotInfoService;

    @PostMapping("/create")
    @Operation(summary = "创建库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理")
    @PreAuthorize("@ss.hasPermission('resource:slot-info:create')")
    public CommonResult<Long> createSlotInfo(@Valid @RequestBody SlotInfoSaveReqVO createReqVO) {
        return success(slotInfoService.createSlotInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理")
    @PreAuthorize("@ss.hasPermission('resource:slot-info:update')")
    public CommonResult<Boolean> updateSlotInfo(@Valid @RequestBody SlotInfoSaveReqVO updateReqVO) {
        slotInfoService.updateSlotInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('resource:slot-info:delete')")
    public CommonResult<Boolean> deleteSlotInfo(@RequestParam("id") Long id) {
        slotInfoService.deleteSlotInfo(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理")
                @PreAuthorize("@ss.hasPermission('resource:slot-info:delete')")
    public CommonResult<Boolean> deleteSlotInfoList(@RequestParam("ids") List<Long> ids) {
        slotInfoService.deleteSlotInfoListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('resource:slot-info:query')")
    public CommonResult<SlotInfoRespVO> getSlotInfo(@RequestParam("id") Long id) {
        SlotInfoDO slotInfo = slotInfoService.getSlotInfo(id);
        return success(BeanUtils.toBean(slotInfo, SlotInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理分页")
    @PreAuthorize("@ss.hasPermission('resource:slot-info:query')")
    public CommonResult<PageResult<SlotInfoRespVO>> getSlotInfoPage(@Valid SlotInfoPageReqVO pageReqVO) {
        PageResult<SlotInfoDO> pageResult = slotInfoService.getSlotInfoPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SlotInfoRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理 Excel")
    @PreAuthorize("@ss.hasPermission('resource:slot-info:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSlotInfoExcel(@Valid SlotInfoPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SlotInfoDO> list = slotInfoService.getSlotInfoPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理.xls", "数据", SlotInfoRespVO.class,
                        BeanUtils.toBean(list, SlotInfoRespVO.class));
    }

}