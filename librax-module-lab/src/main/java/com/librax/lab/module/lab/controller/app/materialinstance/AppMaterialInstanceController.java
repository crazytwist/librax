package com.librax.lab.module.lab.controller.app.materialinstance;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.lab.controller.admin.materialinstance.vo.MaterialInstanceBatchLoadReqVO;
import com.librax.lab.module.lab.controller.admin.materialinstance.vo.MaterialInstanceLoadReqVO;
import com.librax.lab.module.lab.controller.admin.materialinstance.vo.MaterialInstancePageReqVO;
import com.librax.lab.module.lab.controller.admin.materialinstance.vo.MaterialInstanceSaveReqVO;
import com.librax.lab.module.lab.controller.app.materialinstance.vo.AppMaterialInstanceListReqVO;
import com.librax.lab.module.lab.controller.app.materialinstance.vo.AppMaterialInstancePageReqVO;
import com.librax.lab.module.lab.controller.app.materialinstance.vo.AppMaterialInstanceRespVO;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.module.lab.service.materialinstance.MaterialInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 物料实例")
@RestController
@RequestMapping("/lab/material-instance")
@Validated
public class AppMaterialInstanceController {

    @Resource
    private MaterialInstanceService materialInstanceService;

    @PostMapping("/create")
    @Operation(summary = "创建物料实例")
    @PermitAll
    public CommonResult<Long> createMaterialInstance(@Valid @RequestBody MaterialInstanceSaveReqVO createReqVO) {
        return success(materialInstanceService.createMaterialInstance(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新物料实例")
    @PermitAll
    public CommonResult<Boolean> updateMaterialInstance(@Valid @RequestBody MaterialInstanceSaveReqVO updateReqVO) {
        materialInstanceService.updateMaterialInstance(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物料实例")
    @Parameter(name = "id", description = "编号", required = true)
    @PermitAll
    public CommonResult<Boolean> deleteMaterialInstance(@RequestParam("id") Long id) {
        materialInstanceService.deleteMaterialInstance(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除物料实例")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PermitAll
    public CommonResult<Boolean> deleteMaterialInstanceList(@RequestParam("ids") List<Long> ids) {
        materialInstanceService.deleteMaterialInstanceListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得物料实例")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PermitAll
    public CommonResult<AppMaterialInstanceRespVO> getMaterialInstance(@RequestParam("id") Long id) {
        MaterialInstanceDO materialInstance = materialInstanceService.getMaterialInstance(id);
        return success(BeanUtils.toBean(materialInstance, AppMaterialInstanceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得物料实例分页")
    @PermitAll
    public CommonResult<PageResult<AppMaterialInstanceRespVO>> getMaterialInstancePage(
            @Valid AppMaterialInstancePageReqVO pageReqVO) {
        PageResult<MaterialInstanceDO> pageResult = materialInstanceService.getMaterialInstancePage(
                BeanUtils.toBean(pageReqVO, MaterialInstancePageReqVO.class));
        return success(BeanUtils.toBean(pageResult, AppMaterialInstanceRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得物料实例列表")
    @PermitAll
    public CommonResult<List<AppMaterialInstanceRespVO>> getMaterialInstanceList(
            @Valid AppMaterialInstanceListReqVO listReqVO) {
        MaterialInstancePageReqVO pageReqVO = BeanUtils.toBean(listReqVO, MaterialInstancePageReqVO.class);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MaterialInstanceDO> list = materialInstanceService.getMaterialInstancePage(pageReqVO).getList();
        return success(BeanUtils.toBean(list, AppMaterialInstanceRespVO.class));
    }

    @PostMapping("/list-by-slots")
    @Operation(summary = "根据库位ID列表批量查询绑定的物料实例")
    @PermitAll
    public CommonResult<Map<String, AppMaterialInstanceRespVO>> listBySlots(@RequestBody List<String> slotIds) {
        Map<String, MaterialInstanceDO> doMap = materialInstanceService.listBySlotIds(slotIds);
        Map<String, AppMaterialInstanceRespVO> voMap = new LinkedHashMap<>();
        doMap.forEach((slotId, item) -> voMap.put(slotId, BeanUtils.toBean(item, AppMaterialInstanceRespVO.class)));
        return success(voMap);
    }

    @PostMapping("/batch-load")
    @Operation(summary = "批量上架 - 将多个物料实例分别绑定到指定库位")
    @PermitAll
    public CommonResult<List<String>> batchLoadToSlot(@Valid @RequestBody MaterialInstanceBatchLoadReqVO reqVO) {
        List<String> errors = materialInstanceService.batchLoadToSlot(reqVO.getItems());
        return success(errors);
    }

    @PutMapping("/batch-unload")
    @Operation(summary = "批量下架 - 将多个物料实例从当前库位移除")
    @PermitAll
    public CommonResult<Boolean> batchUnloadFromSlot(@RequestBody List<String> instanceIds) {
        materialInstanceService.batchUnloadFromSlot(instanceIds);
        return success(true);
    }

    @PutMapping("/load")
    @Operation(summary = "物料上架 - 将物料实例绑定到指定库位")
    @PermitAll
    public CommonResult<Boolean> loadToSlot(@Valid @RequestBody MaterialInstanceLoadReqVO reqVO) {
        materialInstanceService.loadToSlot(reqVO.getInstanceId(), reqVO.getSlotId());
        return success(true);
    }

    @PutMapping("/unload")
    @Operation(summary = "物料下架 - 将物料实例从当前库位移除")
    @Parameter(name = "instanceId", description = "物料实例ID", required = true)
    @PermitAll
    public CommonResult<Boolean> unloadFromSlot(@RequestParam("instanceId") String instanceId) {
        materialInstanceService.unloadFromSlot(instanceId);
        return success(true);
    }

}
