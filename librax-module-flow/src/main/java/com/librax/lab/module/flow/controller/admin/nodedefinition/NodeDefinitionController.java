package com.librax.lab.module.flow.controller.admin.nodedefinition;

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

import com.librax.lab.module.flow.controller.admin.nodedefinition.vo.*;
import com.librax.lab.module.flow.dal.dataobject.nodedefinition.NodeDefinitionDO;
import com.librax.lab.module.flow.service.nodedefinition.NodeDefinitionService;

@Tag(name = "管理后台 - 流程节点定义")
@RestController
@RequestMapping("/flow/node-definition")
@Validated
public class NodeDefinitionController {

    @Resource
    private NodeDefinitionService nodeDefinitionService;

    @PostMapping("/create")
    @Operation(summary = "创建流程节点定义")
    @PreAuthorize("@ss.hasPermission('flow:node-definition:create')")
    public CommonResult<Long> createNodeDefinition(@Valid @RequestBody NodeDefinitionSaveReqVO createReqVO) {
        return success(nodeDefinitionService.createNodeDefinition(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新流程节点定义")
    @PreAuthorize("@ss.hasPermission('flow:node-definition:update')")
    public CommonResult<Boolean> updateNodeDefinition(@Valid @RequestBody NodeDefinitionSaveReqVO updateReqVO) {
        nodeDefinitionService.updateNodeDefinition(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除流程节点定义")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:node-definition:delete')")
    public CommonResult<Boolean> deleteNodeDefinition(@RequestParam("id") Long id) {
        nodeDefinitionService.deleteNodeDefinition(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除流程节点定义")
                @PreAuthorize("@ss.hasPermission('flow:node-definition:delete')")
    public CommonResult<Boolean> deleteNodeDefinitionList(@RequestParam("ids") List<Long> ids) {
        nodeDefinitionService.deleteNodeDefinitionListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得流程节点定义")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:node-definition:query')")
    public CommonResult<NodeDefinitionRespVO> getNodeDefinition(@RequestParam("id") Long id) {
        NodeDefinitionDO nodeDefinition = nodeDefinitionService.getNodeDefinition(id);
        return success(BeanUtils.toBean(nodeDefinition, NodeDefinitionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得流程节点定义分页")
    @PreAuthorize("@ss.hasPermission('flow:node-definition:query')")
    public CommonResult<PageResult<NodeDefinitionRespVO>> getNodeDefinitionPage(@Valid NodeDefinitionPageReqVO pageReqVO) {
        PageResult<NodeDefinitionDO> pageResult = nodeDefinitionService.getNodeDefinitionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, NodeDefinitionRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出流程节点定义 Excel")
    @PreAuthorize("@ss.hasPermission('flow:node-definition:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportNodeDefinitionExcel(@Valid NodeDefinitionPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<NodeDefinitionDO> list = nodeDefinitionService.getNodeDefinitionPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "流程节点定义.xls", "数据", NodeDefinitionRespVO.class,
                        BeanUtils.toBean(list, NodeDefinitionRespVO.class));
    }

}