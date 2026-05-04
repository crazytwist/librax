package com.librax.lab.module.resource.controller.admin.rackinfo;

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

import com.librax.lab.module.resource.controller.admin.rackinfo.vo.*;
import com.librax.lab.module.resource.dal.dataobject.rackinfo.RackInfoDO;
import com.librax.lab.module.resource.service.rackinfo.RackInfoService;

@Tag(name = "管理后台 - 货架/台面定义，库位的上级容器，归 resource 模块管理")
@RestController
@RequestMapping("/resource/rack-info")
@Validated
public class RackInfoController {

    @Resource
    private RackInfoService rackInfoService;

    @PostMapping("/create")
    @Operation(summary = "创建货架/台面定义，库位的上级容器，归 resource 模块管理")
    @PreAuthorize("@ss.hasPermission('resource:rack-info:create')")
    public CommonResult<Long> createRackInfo(@Valid @RequestBody RackInfoSaveReqVO createReqVO) {
        return success(rackInfoService.createRackInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新货架/台面定义，库位的上级容器，归 resource 模块管理")
    @PreAuthorize("@ss.hasPermission('resource:rack-info:update')")
    public CommonResult<Boolean> updateRackInfo(@Valid @RequestBody RackInfoSaveReqVO updateReqVO) {
        rackInfoService.updateRackInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除货架/台面定义，库位的上级容器，归 resource 模块管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('resource:rack-info:delete')")
    public CommonResult<Boolean> deleteRackInfo(@RequestParam("id") Long id) {
        rackInfoService.deleteRackInfo(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除货架/台面定义，库位的上级容器，归 resource 模块管理")
                @PreAuthorize("@ss.hasPermission('resource:rack-info:delete')")
    public CommonResult<Boolean> deleteRackInfoList(@RequestParam("ids") List<Long> ids) {
        rackInfoService.deleteRackInfoListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得货架/台面定义，库位的上级容器，归 resource 模块管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('resource:rack-info:query')")
    public CommonResult<RackInfoRespVO> getRackInfo(@RequestParam("id") Long id) {
        RackInfoDO rackInfo = rackInfoService.getRackInfo(id);
        return success(BeanUtils.toBean(rackInfo, RackInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得货架/台面定义，库位的上级容器，归 resource 模块管理分页")
    @PreAuthorize("@ss.hasPermission('resource:rack-info:query')")
    public CommonResult<PageResult<RackInfoRespVO>> getRackInfoPage(@Valid RackInfoPageReqVO pageReqVO) {
        PageResult<RackInfoDO> pageResult = rackInfoService.getRackInfoPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, RackInfoRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出货架/台面定义，库位的上级容器，归 resource 模块管理 Excel")
    @PreAuthorize("@ss.hasPermission('resource:rack-info:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRackInfoExcel(@Valid RackInfoPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RackInfoDO> list = rackInfoService.getRackInfoPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "货架/台面定义，库位的上级容器，归 resource 模块管理.xls", "数据", RackInfoRespVO.class,
                        BeanUtils.toBean(list, RackInfoRespVO.class));
    }

}