package com.librax.lab.module.flow.controller.admin.pipelinetrigger;

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

import com.librax.lab.module.flow.controller.admin.pipelinetrigger.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelinetrigger.PipelineTriggerDO;
import com.librax.lab.module.flow.service.pipelinetrigger.PipelineTriggerService;

@Tag(name = "管理后台 - 流程触发配置表，管理定时和事件触发规则 [pd_]")
@RestController
@RequestMapping("/flow/pipeline-trigger")
@Validated
public class PipelineTriggerController {

    @Resource
    private PipelineTriggerService pipelineTriggerService;

    @PostMapping("/create")
    @Operation(summary = "创建流程触发配置表，管理定时和事件触发规则 [pd_]")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-trigger:create')")
    public CommonResult<Long> createPipelineTrigger(@Valid @RequestBody PipelineTriggerSaveReqVO createReqVO) {
        return success(pipelineTriggerService.createPipelineTrigger(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新流程触发配置表，管理定时和事件触发规则 [pd_]")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-trigger:update')")
    public CommonResult<Boolean> updatePipelineTrigger(@Valid @RequestBody PipelineTriggerSaveReqVO updateReqVO) {
        pipelineTriggerService.updatePipelineTrigger(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除流程触发配置表，管理定时和事件触发规则 [pd_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:pipeline-trigger:delete')")
    public CommonResult<Boolean> deletePipelineTrigger(@RequestParam("id") Long id) {
        pipelineTriggerService.deletePipelineTrigger(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除流程触发配置表，管理定时和事件触发规则 [pd_]")
                @PreAuthorize("@ss.hasPermission('flow:pipeline-trigger:delete')")
    public CommonResult<Boolean> deletePipelineTriggerList(@RequestParam("ids") List<Long> ids) {
        pipelineTriggerService.deletePipelineTriggerListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得流程触发配置表，管理定时和事件触发规则 [pd_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-trigger:query')")
    public CommonResult<PipelineTriggerRespVO> getPipelineTrigger(@RequestParam("id") Long id) {
        PipelineTriggerDO pipelineTrigger = pipelineTriggerService.getPipelineTrigger(id);
        return success(BeanUtils.toBean(pipelineTrigger, PipelineTriggerRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得流程触发配置表，管理定时和事件触发规则 [pd_]分页")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-trigger:query')")
    public CommonResult<PageResult<PipelineTriggerRespVO>> getPipelineTriggerPage(@Valid PipelineTriggerPageReqVO pageReqVO) {
        PageResult<PipelineTriggerDO> pageResult = pipelineTriggerService.getPipelineTriggerPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, PipelineTriggerRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出流程触发配置表，管理定时和事件触发规则 [pd_] Excel")
    @PreAuthorize("@ss.hasPermission('flow:pipeline-trigger:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPipelineTriggerExcel(@Valid PipelineTriggerPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PipelineTriggerDO> list = pipelineTriggerService.getPipelineTriggerPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "流程触发配置表，管理定时和事件触发规则 [pd_].xls", "数据", PipelineTriggerRespVO.class,
                        BeanUtils.toBean(list, PipelineTriggerRespVO.class));
    }

}