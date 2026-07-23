package com.librax.lab.module.resource.controller.app.slotinfo;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.tenant.core.aop.TenantIgnore;
import com.librax.lab.module.resource.controller.app.slotinfo.vo.WarehouseCallbackReqVO;
import com.librax.lab.module.resource.service.agvload.AgvLoadPlanService;
import com.librax.lab.module.resource.service.agvload.AgvReturnPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 仓储设备回调接口。
 *
 * <p>供仓储机械臂（WAREHOUSE 设备）在完成搬运动作后主动回调。
 * 补料与下料统一使用同一个回调接口，librax 根据 {@code taskId} 自动路由到对应流程。
 *
 * <p><b>接口概览：</b>
 * <pre>
 * POST /resource/slot/warehouse-callback   仓储操作完成统一回调（补料/下料均用此接口）
 * GET  /resource/slot/load-progress        查询补料进度
 * GET  /resource/slot/return-progress      查询下料进度
 * </pre>
 *
 * <p>所有接口均无需鉴权（@PermitAll），仓储设备可直接调用。
 */
@Slf4j
@Tag(name = "外部设备 - 仓储回调",
        description = "仓储机械臂完成搬运后的统一回调接口，补料与下料共用，无需鉴权")
@RestController
@RequestMapping("/resource/slot")
@Validated
@RequiredArgsConstructor
public class SlotReadyController {

    private final AgvLoadPlanService agvLoadPlanService;
    private final AgvReturnPlanService agvReturnPlanService;

    /**
     * 仓储操作完成统一回调（补料/下料共用）。
     *
     * <p>仓储机械臂完成一次搬运后调用，librax 根据 {@code taskId} 自动判断流程类型并路由：
     * <ul>
     *   <li>taskId 对应<b>补料计划</b> → 中转位已就绪，通知 AGV 来取料</li>
     *   <li>taskId 对应<b>下料计划</b> → 物料已入库，触发同波次下一件或进入下一波次</li>
     *   <li>taskId 为空 → 独立操作，仅更新库位状态</li>
     * </ul>
     *
     * <p><b>必填字段：</b>{@code requestId}、{@code slotId}（中转位ID）、{@code instanceId}、{@code status}
     * <br><b>联动流程必填：</b>{@code taskId}，即 librax 下发 prepareMaterials 时的 {@code transferTaskId}
     * <br><b>可选字段：</b>{@code code}、{@code message}（失败时建议填写）、{@code eventTime}
     *
     * <p><b>字段别名（兼容仓储侧命名习惯）：</b>
     * <ul>
     *   <li>{@code taskId} ↔ {@code transferTaskId}</li>
     *   <li>{@code slotId} ↔ {@code toLocation}（补料）或 {@code fromLocation}（下料），含义相同均为中转位</li>
     *   <li>{@code instanceId} ↔ {@code materialId}</li>
     *   <li>{@code status} 推荐用 {@code SUCCESS} / {@code FAILED}，兼容旧值 {@code READY} / {@code RETURNED}</li>
     * </ul>
     */
    @PostMapping("/warehouse-callback")
    @Operation(summary = "仓储操作完成统一回调（补料/下料共用）",
            description = "必填：requestId、slotId（中转位/toLocation/fromLocation）、instanceId/materialId、status(SUCCESS/FAILED)；联动流程时还需传 taskId/transferTaskId。librax 自动按 taskId 路由到补料或下料逻辑。")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> warehouseCallback(@Valid @RequestBody WarehouseCallbackReqVO req) {
        log.info("[warehouseCallback] 收到仓储回调 requestId={} transferTaskId={} materialId={} toLocation={} fromLocation={} status={}",
                req.getRequestId(), req.getTransferTaskId(), req.getMaterialId(), req.getToLocation(), req.getFromLocation(), req.getStatus());
        if (org.springframework.util.StringUtils.hasText(req.getTransferTaskId())) {
            if (agvLoadPlanService.hasPlan(req.getTransferTaskId())) {
                return CommonResult.success(agvLoadPlanService.markSlotReady(req));
            }
            if (agvReturnPlanService.hasPlan(req.getTransferTaskId())) {
                return CommonResult.success(agvReturnPlanService.markSlotReturned(req));
            }
        }
        // taskId 为空或无匹配计划：独立操作，仅更新库位状态
        return CommonResult.success(agvLoadPlanService.markSlotReady(req));
    }

    /**
     * 查询 AGV 补料进度。
     *
     * @param taskId 补料流程执行ID（即 prepareMaterials 请求中的 transferTaskId）
     */
    @GetMapping("/load-progress")
    @Operation(summary = "查询 AGV 补料进度",
            description = "返回补料计划整体状态及各物料明细的当前搬运状态。taskId 为补料流程 executionId。")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> loadProgress(@RequestParam("taskId") String taskId) {
        return CommonResult.success(agvLoadPlanService.getProgress(taskId));
    }

    /**
     * 查询 AGV 下料进度。
     *
     * @param taskId 下料流程执行ID（即 prepareMaterials 请求中的 transferTaskId）
     */
    @GetMapping("/return-progress")
    @Operation(summary = "查询 AGV 下料进度",
            description = "返回下料计划整体状态（当前波次、已入库数量等）及各物料明细的当前状态。taskId 为下料流程 executionId。")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> returnProgress(@RequestParam("taskId") String taskId) {
        return CommonResult.success(agvReturnPlanService.getProgress(taskId));
    }
}
