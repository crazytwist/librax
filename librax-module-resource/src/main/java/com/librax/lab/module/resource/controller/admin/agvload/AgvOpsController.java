package com.librax.lab.module.resource.controller.admin.agvload;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadPlanDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvReturnPlanDO;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvTaskQueueDO;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvReturnPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvTaskQueueMapper;
import com.librax.lab.module.resource.service.agvload.AgvChargingStateTracker;
import com.librax.lab.module.resource.service.agvload.AgvLoadPlanService;
import com.librax.lab.module.resource.service.agvload.AgvReturnPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - AGV 人工恢复。
 *
 * <p>给排查"AGV卡住不动"这类问题的人工兜底手段：正常情况下事件驱动+若干巡检Job
 * （{@code AgvOrchestrateQueueWatchdog}/{@code AgvWarehouseWindowWatchdog}/
 * {@code AgvTaskQueueWatchdog}）应该都能自愈，这个接口是给它们都没能自愈时、
 * 需要人工介入的场景兜底的，之前一直靠直接手写SQL改表，容易漏步骤（比如忘记清
 * currentAgvTaskId/currentAgvOperation，或者忘记通知流程WAIT节点导致流程执行永远挂着）。
 *
 * <p><b>建议流程：</b>先调用 {@code status} 看清楚当前这台设备的队列/计划/充电状态，
 * 确认要清理什么、大致原因，再对症调用下面的清理接口。
 *
 * <p><b>接口概览：</b>
 * <pre>
 * GET  /resource/agv-ops/status              查看设备当前队列/计划/充电状态
 * POST /resource/agv-ops/cancel-load-plan     强制终止一个卡死的补料计划
 * POST /resource/agv-ops/cancel-return-plan   强制终止一个卡死的下料计划
 * POST /resource/agv-ops/clear-device-queue   清空设备的AGV指令队列（不区分计划，一次性全清）
 * POST /resource/agv-ops/reset-charging-state 清除"AGV正在被系统充电"的内存标记
 * </pre>
 *
 * <p><b>注意：</b>这里只处理 AGV 设备队列（{@code lab_agv_task_queue}）、补料/下料计划表
 * 和 {@link AgvChargingStateTracker} 这三类状态。流程引擎自己的资源池锁
 * （Redis {@code resource:lock:*}/{@code resource:holder:*}，跟AGV/库位槽位的抢占排队
 * 有关，不是这几个类管理的）如果也卡住了，需要用 {@code clear_agv_redis_cache.sh} 单独排查，
 * 强制终止计划这里已经会通过 stepCallbackSpi 通知流程失败，正常情况下流程引擎会自己释放
 * 对应的资源池占用。
 */
@Slf4j
@Tag(name = "管理后台 - AGV人工恢复", description = "AGV卡死时的人工兜底恢复接口：查看状态、强制终止计划、清队列、清内存充电标记")
@RestController
@RequestMapping("/resource/agv-ops")
@Validated
public class AgvOpsController {

    @Resource
    private AgvTaskQueueMapper queueMapper;
    @Resource
    private AgvLoadPlanMapper loadPlanMapper;
    @Resource
    private AgvReturnPlanMapper returnPlanMapper;
    @Resource
    private AgvLoadPlanService loadPlanService;
    @Resource
    private AgvReturnPlanService returnPlanService;
    @Resource
    private AgvChargingStateTracker chargingStateTracker;

    @GetMapping("/status")
    @Operation(summary = "查看设备当前AGV队列/计划/充电状态", description = "人工恢复前先看一眼，确认要清理什么")
    @Parameter(name = "deviceId", description = "AGV设备ID", required = true, example = "AGV-01")
    @PreAuthorize("@ss.hasPermission('resource:agv-ops:query')")
    public CommonResult<Map<String, Object>> status(@RequestParam("deviceId") String deviceId) {
        List<AgvTaskQueueDO> queueEntries = queueMapper.selectAllByDeviceId(deviceId);
        List<AgvLoadPlanDO> activeLoadPlans = loadPlanMapper.selectActiveByDeviceId(deviceId);
        List<AgvReturnPlanDO> activeReturnPlans = returnPlanMapper.selectActiveByDeviceId(deviceId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceId", deviceId);
        result.put("hasDispatched", queueMapper.hasDispatched(deviceId));
        result.put("hasPending", queueMapper.hasPending(deviceId));
        result.put("chargingFlag", chargingStateTracker.isCharging(deviceId));
        result.put("queueEntries", queueEntries);
        result.put("activeLoadPlans", activeLoadPlans);
        result.put("activeReturnPlans", activeReturnPlans);
        return success(result);
    }

    @PostMapping("/cancel-load-plan")
    @Operation(summary = "强制终止一个卡死的补料计划", description = "状态置FAILED、取消关联队列条目、释放槽位、通知流程WAIT节点失败；已是终态则不做任何事")
    @Parameter(name = "taskId", description = "补料计划taskId（即流程executionId）", required = true)
    @Parameter(name = "reason", description = "终止原因，写进流程回调的errorMsg里，便于事后排查")
    @PreAuthorize("@ss.hasPermission('resource:agv-ops:reset')")
    public CommonResult<Boolean> cancelLoadPlan(@RequestParam("taskId") String taskId,
                                                 @RequestParam(value = "reason", required = false) String reason) {
        boolean cancelled = loadPlanService.forceCancel(taskId, reason);
        log.warn("[AgvOps] 人工强制终止补料计划 taskId={} reason={} 实际执行={}", taskId, reason, cancelled);
        return success(cancelled);
    }

    @PostMapping("/cancel-return-plan")
    @Operation(summary = "强制终止一个卡死的下料计划", description = "状态置FAILED、取消关联队列条目、释放槽位、通知流程WAIT节点失败；已是终态则不做任何事")
    @Parameter(name = "taskId", description = "下料计划taskId（即流程executionId）", required = true)
    @Parameter(name = "reason", description = "终止原因，写进流程回调的errorMsg里，便于事后排查")
    @PreAuthorize("@ss.hasPermission('resource:agv-ops:reset')")
    public CommonResult<Boolean> cancelReturnPlan(@RequestParam("taskId") String taskId,
                                                   @RequestParam(value = "reason", required = false) String reason) {
        boolean cancelled = returnPlanService.forceCancel(taskId, reason);
        log.warn("[AgvOps] 人工强制终止下料计划 taskId={} reason={} 实际执行={}", taskId, reason, cancelled);
        return success(cancelled);
    }

    @PostMapping("/clear-device-queue")
    @Operation(summary = "清空设备的AGV指令队列", description = "把该设备所有PENDING/DISPATCHED队列项标记CANCELLED，不区分属于哪个计划，一次性全清。"
            + "只清队列，不改计划状态——如果计划本身也卡住了，需要另外调用 cancel-load-plan/cancel-return-plan")
    @Parameter(name = "deviceId", description = "AGV设备ID", required = true, example = "AGV-01")
    @PreAuthorize("@ss.hasPermission('resource:agv-ops:reset')")
    public CommonResult<Boolean> clearDeviceQueue(@RequestParam("deviceId") String deviceId) {
        queueMapper.cancelAllByDeviceId(deviceId);
        log.warn("[AgvOps] 人工清空设备队列 deviceId={}", deviceId);
        return success(true);
    }

    @PostMapping("/reset-charging-state")
    @Operation(summary = "清除\"AGV正在被系统充电\"的内存标记", description = "纯内存态，应用重启会自动清空；这里是不重启应用时的手动清除入口，"
            + "比如怀疑AgvQueueService因为这个标记一直误发停止充电指令时用")
    @Parameter(name = "deviceId", description = "AGV设备ID", required = true, example = "AGV-01")
    @PreAuthorize("@ss.hasPermission('resource:agv-ops:reset')")
    public CommonResult<Boolean> resetChargingState(@RequestParam("deviceId") String deviceId) {
        chargingStateTracker.markNotCharging(deviceId);
        log.warn("[AgvOps] 人工清除充电内存标记 deviceId={}", deviceId);
        return success(true);
    }
}
