package com.librax.lab.module.resource.controller.admin.resourceconfig;

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

import com.librax.lab.module.resource.controller.admin.resourceconfig.vo.*;
import com.librax.lab.module.resource.dal.dataobject.resourceconfig.ResourceConfigDO;
import com.librax.lab.module.resource.service.resourceconfig.ResourceConfigService;

@Tag(name = "管理后台 - 资源配置表,运行时锁状态见Redis")
@RestController
@RequestMapping("/resource/config")
@Validated
public class ResourceConfigController {

    @Resource
    private ResourceConfigService configService;

    @PostMapping("/create")
    @Operation(summary = "创建资源配置表,运行时锁状态见Redis")
    @PreAuthorize("@ss.hasPermission('resource:config:create')")
    public CommonResult<Long> createConfig(@Valid @RequestBody ResourceConfigSaveReqVO createReqVO) {
        return success(configService.createConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资源配置表,运行时锁状态见Redis")
    @PreAuthorize("@ss.hasPermission('resource:config:update')")
    public CommonResult<Boolean> updateConfig(@Valid @RequestBody ResourceConfigSaveReqVO updateReqVO) {
        configService.updateConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资源配置表,运行时锁状态见Redis")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('resource:config:delete')")
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        configService.deleteConfig(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除资源配置表,运行时锁状态见Redis")
                @PreAuthorize("@ss.hasPermission('resource:config:delete')")
    public CommonResult<Boolean> deleteConfigList(@RequestParam("ids") List<Long> ids) {
        configService.deleteConfigListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资源配置表,运行时锁状态见Redis")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('resource:config:query')")
    public CommonResult<ResourceConfigRespVO> getConfig(@RequestParam("id") Long id) {
        ResourceConfigDO config = configService.getConfig(id);
        return success(BeanUtils.toBean(config, ResourceConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资源配置表,运行时锁状态见Redis分页")
    @PreAuthorize("@ss.hasPermission('resource:config:query')")
    public CommonResult<PageResult<ResourceConfigRespVO>> getConfigPage(@Valid ResourceConfigPageReqVO pageReqVO) {
        PageResult<ResourceConfigDO> pageResult = configService.getConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ResourceConfigRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资源配置表,运行时锁状态见Redis Excel")
    @PreAuthorize("@ss.hasPermission('resource:config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportConfigExcel(@Valid ResourceConfigPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ResourceConfigDO> list = configService.getConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "资源配置表,运行时锁状态见Redis.xls", "数据", ResourceConfigRespVO.class,
                        BeanUtils.toBean(list, ResourceConfigRespVO.class));
    }

}