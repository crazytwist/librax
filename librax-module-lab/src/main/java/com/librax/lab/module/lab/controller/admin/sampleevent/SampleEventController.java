package com.librax.lab.module.lab.controller.admin.sampleevent;

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

import com.librax.lab.module.lab.controller.admin.sampleevent.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleEventDO;
import com.librax.lab.module.lab.service.sampleevent.SampleEventService;

@Tag(name = "管理后台 - 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]")
@RestController
@RequestMapping("/lab/sample-event")
@Validated
public class SampleEventController {

    @Resource
    private SampleEventService sampleEventService;

    @PostMapping("/create")
    @Operation(summary = "创建样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]")
    @PreAuthorize("@ss.hasPermission('lab:sample-event:create')")
    public CommonResult<Long> createSampleEvent(@Valid @RequestBody SampleEventSaveReqVO createReqVO) {
        return success(sampleEventService.createSampleEvent(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]")
    @PreAuthorize("@ss.hasPermission('lab:sample-event:update')")
    public CommonResult<Boolean> updateSampleEvent(@Valid @RequestBody SampleEventSaveReqVO updateReqVO) {
        sampleEventService.updateSampleEvent(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:sample-event:delete')")
    public CommonResult<Boolean> deleteSampleEvent(@RequestParam("id") Long id) {
        sampleEventService.deleteSampleEvent(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]")
                @PreAuthorize("@ss.hasPermission('lab:sample-event:delete')")
    public CommonResult<Boolean> deleteSampleEventList(@RequestParam("ids") List<Long> ids) {
        sampleEventService.deleteSampleEventListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:sample-event:query')")
    public CommonResult<SampleEventRespVO> getSampleEvent(@RequestParam("id") Long id) {
        SampleEventDO sampleEvent = sampleEventService.getSampleEvent(id);
        return success(BeanUtils.toBean(sampleEvent, SampleEventRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]分页")
    @PreAuthorize("@ss.hasPermission('lab:sample-event:query')")
    public CommonResult<PageResult<SampleEventRespVO>> getSampleEventPage(@Valid SampleEventPageReqVO pageReqVO) {
        PageResult<SampleEventDO> pageResult = sampleEventService.getSampleEventPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SampleEventRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_] Excel")
    @PreAuthorize("@ss.hasPermission('lab:sample-event:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSampleEventExcel(@Valid SampleEventPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SampleEventDO> list = sampleEventService.getSampleEventPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_].xls", "数据", SampleEventRespVO.class,
                        BeanUtils.toBean(list, SampleEventRespVO.class));
    }

}