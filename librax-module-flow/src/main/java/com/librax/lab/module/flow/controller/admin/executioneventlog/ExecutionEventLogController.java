package com.librax.lab.module.flow.controller.admin.executioneventlog;

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

import com.librax.lab.module.flow.controller.admin.executioneventlog.vo.*;
import com.librax.lab.module.flow.dal.dataobject.executioneventlog.ExecutionEventLogDO;
import com.librax.lab.module.flow.service.executioneventlog.ExecutionEventLogService;

@Tag(name = "管理后台 - 执行事件日志，只 INSERT 不修改，全链路追踪与审计")
@RestController
@RequestMapping("/flow/execution-event-log")
@Validated
public class ExecutionEventLogController {

    @Resource
    private ExecutionEventLogService executionEventLogService;

    @PostMapping("/create")
    @Operation(summary = "创建执行事件日志，只 INSERT 不修改，全链路追踪与审计")
    @PreAuthorize("@ss.hasPermission('flow:execution-event-log:create')")
    public CommonResult<Long> createExecutionEventLog(@Valid @RequestBody ExecutionEventLogSaveReqVO createReqVO) {
        return success(executionEventLogService.createExecutionEventLog(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新执行事件日志，只 INSERT 不修改，全链路追踪与审计")
    @PreAuthorize("@ss.hasPermission('flow:execution-event-log:update')")
    public CommonResult<Boolean> updateExecutionEventLog(@Valid @RequestBody ExecutionEventLogSaveReqVO updateReqVO) {
        executionEventLogService.updateExecutionEventLog(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除执行事件日志，只 INSERT 不修改，全链路追踪与审计")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:execution-event-log:delete')")
    public CommonResult<Boolean> deleteExecutionEventLog(@RequestParam("id") Long id) {
        executionEventLogService.deleteExecutionEventLog(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除执行事件日志，只 INSERT 不修改，全链路追踪与审计")
                @PreAuthorize("@ss.hasPermission('flow:execution-event-log:delete')")
    public CommonResult<Boolean> deleteExecutionEventLogList(@RequestParam("ids") List<Long> ids) {
        executionEventLogService.deleteExecutionEventLogListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得执行事件日志，只 INSERT 不修改，全链路追踪与审计")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:execution-event-log:query')")
    public CommonResult<ExecutionEventLogRespVO> getExecutionEventLog(@RequestParam("id") Long id) {
        ExecutionEventLogDO executionEventLog = executionEventLogService.getExecutionEventLog(id);
        return success(BeanUtils.toBean(executionEventLog, ExecutionEventLogRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得执行事件日志，只 INSERT 不修改，全链路追踪与审计分页")
    @PreAuthorize("@ss.hasPermission('flow:execution-event-log:query')")
    public CommonResult<PageResult<ExecutionEventLogRespVO>> getExecutionEventLogPage(@Valid ExecutionEventLogPageReqVO pageReqVO) {
        PageResult<ExecutionEventLogDO> pageResult = executionEventLogService.getExecutionEventLogPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ExecutionEventLogRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出执行事件日志，只 INSERT 不修改，全链路追踪与审计 Excel")
    @PreAuthorize("@ss.hasPermission('flow:execution-event-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExecutionEventLogExcel(@Valid ExecutionEventLogPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ExecutionEventLogDO> list = executionEventLogService.getExecutionEventLogPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "执行事件日志，只 INSERT 不修改，全链路追踪与审计.xls", "数据", ExecutionEventLogRespVO.class,
                        BeanUtils.toBean(list, ExecutionEventLogRespVO.class));
    }

}