package com.librax.lab.module.task.controller.admin.taskexecutorconfig;

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

import com.librax.lab.module.task.controller.admin.taskexecutorconfig.vo.*;
import com.librax.lab.module.task.dal.dataobject.taskexecutorconfig.TaskExecutorConfigDO;
import com.librax.lab.module.task.service.taskexecutorconfig.TaskExecutorConfigService;

@Tag(name = "管理后台 - 执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]")
@RestController
@RequestMapping("/task/executor-config")
@Validated
public class TaskExecutorConfigController {

    @Resource
    private TaskExecutorConfigService executorConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]")
    @PreAuthorize("@ss.hasPermission('task:executor-config:create')")
    public CommonResult<Long> createExecutorConfig(@Valid @RequestBody TaskExecutorConfigSaveReqVO createReqVO) {
        return success(executorConfigService.createExecutorConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]")
    @PreAuthorize("@ss.hasPermission('task:executor-config:update')")
    public CommonResult<Boolean> updateExecutorConfig(@Valid @RequestBody TaskExecutorConfigSaveReqVO updateReqVO) {
        executorConfigService.updateExecutorConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('task:executor-config:delete')")
    public CommonResult<Boolean> deleteExecutorConfig(@RequestParam("id") Long id) {
        executorConfigService.deleteExecutorConfig(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]")
                @PreAuthorize("@ss.hasPermission('task:executor-config:delete')")
    public CommonResult<Boolean> deleteExecutorConfigList(@RequestParam("ids") List<Long> ids) {
        executorConfigService.deleteExecutorConfigListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('task:executor-config:query')")
    public CommonResult<TaskExecutorConfigRespVO> getExecutorConfig(@RequestParam("id") Long id) {
        TaskExecutorConfigDO executorConfig = executorConfigService.getExecutorConfig(id);
        return success(BeanUtils.toBean(executorConfig, TaskExecutorConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_]分页")
    @PreAuthorize("@ss.hasPermission('task:executor-config:query')")
    public CommonResult<PageResult<TaskExecutorConfigRespVO>> getExecutorConfigPage(@Valid TaskExecutorConfigPageReqVO pageReqVO) {
        PageResult<TaskExecutorConfigDO> pageResult = executorConfigService.getExecutorConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TaskExecutorConfigRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_] Excel")
    @PreAuthorize("@ss.hasPermission('task:executor-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExecutorConfigExcel(@Valid TaskExecutorConfigPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaskExecutorConfigDO> list = executorConfigService.getExecutorConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "执行器配置表，控制各类型任务的并发/限流/超时 [lab_task_].xls", "数据", TaskExecutorConfigRespVO.class,
                        BeanUtils.toBean(list, TaskExecutorConfigRespVO.class));
    }

}