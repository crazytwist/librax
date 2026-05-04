package com.librax.lab.module.lab.controller.admin.materialcheckrule;

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

import com.librax.lab.module.lab.controller.admin.materialcheckrule.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialcheckrule.MaterialCheckRuleDO;
import com.librax.lab.module.lab.service.materialcheckrule.MaterialCheckRuleService;

@Tag(name = "管理后台 - 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理")
@RestController
@RequestMapping("/lab/material-check-rule")
@Validated
public class MaterialCheckRuleController {

    @Resource
    private MaterialCheckRuleService materialCheckRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:material-check-rule:create')")
    public CommonResult<Long> createMaterialCheckRule(@Valid @RequestBody MaterialCheckRuleSaveReqVO createReqVO) {
        return success(materialCheckRuleService.createMaterialCheckRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理")
    @PreAuthorize("@ss.hasPermission('lab:material-check-rule:update')")
    public CommonResult<Boolean> updateMaterialCheckRule(@Valid @RequestBody MaterialCheckRuleSaveReqVO updateReqVO) {
        materialCheckRuleService.updateMaterialCheckRule(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('lab:material-check-rule:delete')")
    public CommonResult<Boolean> deleteMaterialCheckRule(@RequestParam("id") Long id) {
        materialCheckRuleService.deleteMaterialCheckRule(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理")
                @PreAuthorize("@ss.hasPermission('lab:material-check-rule:delete')")
    public CommonResult<Boolean> deleteMaterialCheckRuleList(@RequestParam("ids") List<Long> ids) {
        materialCheckRuleService.deleteMaterialCheckRuleListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lab:material-check-rule:query')")
    public CommonResult<MaterialCheckRuleRespVO> getMaterialCheckRule(@RequestParam("id") Long id) {
        MaterialCheckRuleDO materialCheckRule = materialCheckRuleService.getMaterialCheckRule(id);
        return success(BeanUtils.toBean(materialCheckRule, MaterialCheckRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理分页")
    @PreAuthorize("@ss.hasPermission('lab:material-check-rule:query')")
    public CommonResult<PageResult<MaterialCheckRuleRespVO>> getMaterialCheckRulePage(@Valid MaterialCheckRulePageReqVO pageReqVO) {
        PageResult<MaterialCheckRuleDO> pageResult = materialCheckRuleService.getMaterialCheckRulePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MaterialCheckRuleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理 Excel")
    @PreAuthorize("@ss.hasPermission('lab:material-check-rule:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMaterialCheckRuleExcel(@Valid MaterialCheckRulePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MaterialCheckRuleDO> list = materialCheckRuleService.getMaterialCheckRulePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理.xls", "数据", MaterialCheckRuleRespVO.class,
                        BeanUtils.toBean(list, MaterialCheckRuleRespVO.class));
    }

}