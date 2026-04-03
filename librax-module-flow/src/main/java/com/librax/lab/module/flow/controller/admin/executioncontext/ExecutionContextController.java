package com.librax.lab.module.flow.controller.admin.executioncontext;

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

import com.librax.lab.module.flow.controller.admin.executioncontext.vo.*;
import com.librax.lab.module.flow.dal.dataobject.executioncontext.ExecutionContextDO;
import com.librax.lab.module.flow.service.executioncontext.ExecutionContextService;

@Tag(name = "管理后台 - 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用")
@RestController
@RequestMapping("/flow/execution-context")
@Validated
public class ExecutionContextController {

    @Resource
    private ExecutionContextService executionContextService;

    @PostMapping("/create")
    @Operation(summary = "创建执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用")
    @PreAuthorize("@ss.hasPermission('flow:execution-context:create')")
    public CommonResult<Long> createExecutionContext(@Valid @RequestBody ExecutionContextSaveReqVO createReqVO) {
        return success(executionContextService.createExecutionContext(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用")
    @PreAuthorize("@ss.hasPermission('flow:execution-context:update')")
    public CommonResult<Boolean> updateExecutionContext(@Valid @RequestBody ExecutionContextSaveReqVO updateReqVO) {
        executionContextService.updateExecutionContext(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('flow:execution-context:delete')")
    public CommonResult<Boolean> deleteExecutionContext(@RequestParam("id") Long id) {
        executionContextService.deleteExecutionContext(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用")
                @PreAuthorize("@ss.hasPermission('flow:execution-context:delete')")
    public CommonResult<Boolean> deleteExecutionContextList(@RequestParam("ids") List<Long> ids) {
        executionContextService.deleteExecutionContextListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('flow:execution-context:query')")
    public CommonResult<ExecutionContextRespVO> getExecutionContext(@RequestParam("id") Long id) {
        ExecutionContextDO executionContext = executionContextService.getExecutionContext(id);
        return success(BeanUtils.toBean(executionContext, ExecutionContextRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用分页")
    @PreAuthorize("@ss.hasPermission('flow:execution-context:query')")
    public CommonResult<PageResult<ExecutionContextRespVO>> getExecutionContextPage(@Valid ExecutionContextPageReqVO pageReqVO) {
        PageResult<ExecutionContextDO> pageResult = executionContextService.getExecutionContextPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ExecutionContextRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用 Excel")
    @PreAuthorize("@ss.hasPermission('flow:execution-context:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExecutionContextExcel(@Valid ExecutionContextPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ExecutionContextDO> list = executionContextService.getExecutionContextPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用.xls", "数据", ExecutionContextRespVO.class,
                        BeanUtils.toBean(list, ExecutionContextRespVO.class));
    }

}