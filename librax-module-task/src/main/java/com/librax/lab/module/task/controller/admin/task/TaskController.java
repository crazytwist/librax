package com.librax.lab.module.task.controller.admin.task;

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

import com.librax.lab.module.task.controller.admin.task.vo.*;
import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.service.task.TaskService;

@Tag(name = "管理后台 - 统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]")
@RestController
@RequestMapping("/task/task")
@Validated
public class TaskController {

    @Resource
    private TaskService taskService;

    @PostMapping("/create")
    @Operation(summary = "创建统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]")
    @PreAuthorize("@ss.hasPermission('task:task:create')")
    public CommonResult<Long> createTask(@Valid @RequestBody TaskSaveReqVO createReqVO) {
        return success(taskService.createTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]")
    @PreAuthorize("@ss.hasPermission('task:task:update')")
    public CommonResult<Boolean> updateTask(@Valid @RequestBody TaskSaveReqVO updateReqVO) {
        taskService.updateTask(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('task:task:delete')")
    public CommonResult<Boolean> deleteTask(@RequestParam("id") Long id) {
        taskService.deleteTask(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]")
                @PreAuthorize("@ss.hasPermission('task:task:delete')")
    public CommonResult<Boolean> deleteTaskList(@RequestParam("ids") List<Long> ids) {
        taskService.deleteTaskListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('task:task:query')")
    public CommonResult<TaskRespVO> getTask(@RequestParam("id") Long id) {
        TaskDO task = taskService.getTask(id);
        return success(BeanUtils.toBean(task, TaskRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_]分页")
    @PreAuthorize("@ss.hasPermission('task:task:query')")
    public CommonResult<PageResult<TaskRespVO>> getTaskPage(@Valid TaskPageReqVO pageReqVO) {
        PageResult<TaskDO> pageResult = taskService.getTaskPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TaskRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_] Excel")
    @PreAuthorize("@ss.hasPermission('task:task:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTaskExcel(@Valid TaskPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaskDO> list = taskService.getTaskPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "统一任务主表，承载所有类型的执行任务，是引擎与执行单元之间的协议层 [lab_task_].xls", "数据", TaskRespVO.class,
                        BeanUtils.toBean(list, TaskRespVO.class));
    }

}