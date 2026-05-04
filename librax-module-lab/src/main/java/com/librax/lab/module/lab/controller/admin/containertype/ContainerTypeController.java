package com.librax.lab.module.lab.controller.admin.containertype;

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

import com.librax.lab.module.lab.controller.admin.containertype.vo.*;
import com.librax.lab.module.lab.dal.dataobject.containertype.ContainerTypeDO;
import com.librax.lab.module.lab.service.containertype.ContainerTypeService;

@Tag(name = "管理后台 - 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理")
@RestController
@RequestMapping("/lab/container-type")
@Validated
public class ContainerTypeController {

    @Resource
    private ContainerTypeService containerTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:container-type:create')")
    public CommonResult<Long> createContainerType(@Valid @RequestBody ContainerTypeSaveReqVO createReqVO) {
        return success(containerTypeService.createContainerType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:container-type:update')")
    public CommonResult<Boolean> updateContainerType(@Valid @RequestBody ContainerTypeSaveReqVO updateReqVO) {
        containerTypeService.updateContainerType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:container-type:delete')")
    public CommonResult<Boolean> deleteContainerType(@RequestParam("id") Long id) {
        containerTypeService.deleteContainerType(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理")
                @PreAuthorize("@ss.hasPermission('lab:container-type:delete')")
    public CommonResult<Boolean> deleteContainerTypeList(@RequestParam("ids") List<Long> ids) {
        containerTypeService.deleteContainerTypeListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:container-type:query')")
    public CommonResult<ContainerTypeRespVO> getContainerType(@RequestParam("id") Long id) {
        ContainerTypeDO containerType = containerTypeService.getContainerType(id);
        return success(BeanUtils.toBean(containerType, ContainerTypeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理分页")
    @PreAuthorize("@ss.hasPermission('lab:container-type:query')")
    public CommonResult<PageResult<ContainerTypeRespVO>> getContainerTypePage(@Valid ContainerTypePageReqVO pageReqVO) {
        PageResult<ContainerTypeDO> pageResult = containerTypeService.getContainerTypePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ContainerTypeRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理 Excel")
    @PreAuthorize("@ss.hasPermission('lab:container-type:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportContainerTypeExcel(@Valid ContainerTypePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ContainerTypeDO> list = containerTypeService.getContainerTypePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理.xls", "数据", ContainerTypeRespVO.class,
                        BeanUtils.toBean(list, ContainerTypeRespVO.class));
    }

}