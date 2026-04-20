package com.librax.lab.module.task.controller.admin.taskevent;

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

import com.librax.lab.module.task.controller.admin.taskevent.vo.*;
import com.librax.lab.module.task.dal.dataobject.taskevent.TaskEventDO;
import com.librax.lab.module.task.service.taskevent.TaskEventService;

@Tag(name = "管理后台 - 任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]")
@RestController
@RequestMapping("/task/event")
@Validated
public class TaskEventController {

    @Resource
    private TaskEventService eventService;

    @PostMapping("/create")
    @Operation(summary = "创建任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]")
    @PreAuthorize("@ss.hasPermission('task:event:create')")
    public CommonResult<Long> createEvent(@Valid @RequestBody TaskEventSaveReqVO createReqVO) {
        return success(eventService.createEvent(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]")
    @PreAuthorize("@ss.hasPermission('task:event:update')")
    public CommonResult<Boolean> updateEvent(@Valid @RequestBody TaskEventSaveReqVO updateReqVO) {
        eventService.updateEvent(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('task:event:delete')")
    public CommonResult<Boolean> deleteEvent(@RequestParam("id") Long id) {
        eventService.deleteEvent(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]")
                @PreAuthorize("@ss.hasPermission('task:event:delete')")
    public CommonResult<Boolean> deleteEventList(@RequestParam("ids") List<Long> ids) {
        eventService.deleteEventListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('task:event:query')")
    public CommonResult<TaskEventRespVO> getEvent(@RequestParam("id") Long id) {
        TaskEventDO event = eventService.getEvent(id);
        return success(BeanUtils.toBean(event, TaskEventRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得任务事件日志，INSERT-ONLY，全链路审计 [lab_task_]分页")
    @PreAuthorize("@ss.hasPermission('task:event:query')")
    public CommonResult<PageResult<TaskEventRespVO>> getEventPage(@Valid TaskEventPageReqVO pageReqVO) {
        PageResult<TaskEventDO> pageResult = eventService.getEventPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TaskEventRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出任务事件日志，INSERT-ONLY，全链路审计 [lab_task_] Excel")
    @PreAuthorize("@ss.hasPermission('task:event:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportEventExcel(@Valid TaskEventPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaskEventDO> list = eventService.getEventPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "任务事件日志，INSERT-ONLY，全链路审计 [lab_task_].xls", "数据", TaskEventRespVO.class,
                        BeanUtils.toBean(list, TaskEventRespVO.class));
    }

}