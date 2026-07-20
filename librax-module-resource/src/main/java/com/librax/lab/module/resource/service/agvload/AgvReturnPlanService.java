package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.module.resource.controller.app.slotinfo.vo.WarehouseCallbackReqVO;

import java.util.List;
import java.util.Map;

/**
 * AGV 下料编排服务。
 *
 * <p>流程：AGV 从站位取料（LOAD）→ 移动到仓储区（MOVE）
 * → 按波次卸料到中转位（UNLOAD × N）→ 仓储机械臂逐件入库（returnMaterials × N）
 * → 流程回调 → commitReturn。
 */
public interface AgvReturnPlanService {

    /**
     * 启动下料编排。
     *
     * <p>入参 {@code returnItems} 每个元素包含：
     * <ul>
     *   <li>instanceId</li>
     *   <li>containerType</li>
     *   <li>sourceSlotId - 站位库位（AGV从此取料）</li>
     *   <li>agvSlotId - AGV载台槽位</li>
     *   <li>transitSlotId - 仓储中转位（AGV卸料到此）</li>
     *   <li>warehouseTargetLocation - 仓储目标货架</li>
     *   <li>step1 - LOAD机械臂指令</li>
     *   <li>step2 - MOVE指令（仅第一件需要）</li>
     *   <li>step3 - UNLOAD机械臂指令</li>
     * </ul>
     *
     * @param taskId       流程执行ID
     * @param nodeId       流程编排等待节点ID
     * @param returnItems  下料物料列表
     * @param inputParams  流程输入参数（transitCapacity、agvDeviceId 等）
     */
    void startReturnOrchestration(String taskId, String nodeId,
                                   List<Map<String, Object>> returnItems,
                                   Map<String, Object> inputParams);

    /**
     * 仓储机械臂完成单件入库后的回调。
     *
     * <p>仓储将物料从中转位放回货架后调用此接口，服务端判断当前波次是否全部完成：
     * 若有下一件 → 继续请求仓储；若波次完成且有下一波 → 下发 AGV UNLOAD；若全部完成 → 唤醒流程。
     */
    Map<String, Object> markSlotReturned(WarehouseCallbackReqVO req);

    /** 提交下料结果，清理库位占用（由流程 agv_commit_return 步骤调用）。 */
    void commitReturn(String taskId);

    boolean hasPlan(String taskId);

    Map<String, Object> getProgress(String taskId);
}
