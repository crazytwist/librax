package com.librax.lab.module.flow.controller.admin.definition;

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

import com.librax.lab.module.flow.controller.admin.definition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.definition.FlowDefinitionDO;
import com.librax.lab.module.flow.service.definition.DefinitionService;

@Tag(name = "管理后台 - 流程定义")
@RestController
@RequestMapping("/flow/definition")
@Validated
public class DefinitionController {

    @Resource
    private DefinitionService definitionService;

    @PostMapping("/create")
    @Operation(summary = "创建流程定义")
    @PreAuthorize("@ss.hasPermission('flow:definition:create')")
    public CommonResult<Long> createDefinition(@Valid @RequestBody DefinitionSaveReqVO createReqVO) {
        return success(definitionService.createDefinition(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新流程定义")
    @PreAuthorize("@ss.hasPermission('flow:definition:update')")
    public CommonResult<Boolean> updateDefinition(@Valid @RequestBody DefinitionSaveReqVO updateReqVO) {
        definitionService.updateDefinition(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除流程定义")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:definition:delete')")
    public CommonResult<Boolean> deleteDefinition(@RequestParam("id") Long id) {
        definitionService.deleteDefinition(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除流程定义")
                @PreAuthorize("@ss.hasPermission('flow:definition:delete')")
    public CommonResult<Boolean> deleteDefinitionList(@RequestParam("ids") List<Long> ids) {
        definitionService.deleteDefinitionListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得流程定义")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:definition:query')")
    public CommonResult<DefinitionRespVO> getDefinition(@RequestParam("id") Long id) {
        FlowDefinitionDO definition = definitionService.getDefinition(id);
        return success(BeanUtils.toBean(definition, DefinitionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得流程定义分页")
    @PreAuthorize("@ss.hasPermission('flow:definition:query')")
    public CommonResult<PageResult<DefinitionRespVO>> getDefinitionPage(@Valid DefinitionPageReqVO pageReqVO) {
        PageResult<FlowDefinitionDO> pageResult = definitionService.getDefinitionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DefinitionRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出流程定义 Excel")
    @PreAuthorize("@ss.hasPermission('flow:definition:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDefinitionExcel(@Valid DefinitionPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<FlowDefinitionDO> list = definitionService.getDefinitionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "流程定义.xls", "数据", DefinitionRespVO.class,
                        BeanUtils.toBean(list, DefinitionRespVO.class));
    }

}