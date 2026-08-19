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
     * {@link #startReturnOrchestration} 的实际尝试实现，独立事务（REQUIRES_NEW）：
     * 抢占AGV/中转槽位失败时抛出的异常只回滚这一次尝试。仅供 {@link #startReturnOrchestration}
     * 和 {@link #tryDispatchQueuedOrchestrationAsync} 内部调用，不直接对外暴露。
     */
    void attemptStartReturnOrchestration(String taskId, String nodeId,
                                         List<Map<String, Object>> returnItems,
                                         Map<String, Object> inputParams);

    /**
     * 事务提交后异步尝试从 {@code lab_agv_orchestrate_queue} 里取出最早一条排队的下料编排请求重试。
     * 由所有会释放AGV/中转槽位的地方（如 {@link #markSlotReturned}）触发。
     */
    void tryDispatchQueuedOrchestrationAsync(String deviceId);

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

    /**
     * 事务提交后异步触发下一步：请求仓储入库、下发下一波 AGV 卸料、或回调流程。
     *
     * <p>由 {@link #markSlotReturned} 在事务提交后通过 {@code afterCommit} 钩子调用，
     * 确保先返回 200 给仓储，再执行后续外部调用，与补料侧保持一致。
     *
     * @param taskId 下料编排任务ID（即流程执行ID）
     */
    void triggerNextStepAsync(String taskId);

    /**
     * 事务提交后异步推进AGV下料分阶段任务的下一步（LOAD->MOVE / MOVE->UNLOAD / UNLOAD->完成），
     * 确保 taskState() 先把ACK返回给AGV，再发下一条 startTask 指令。
     */
    void advanceAfterTaskStateAsync(String taskId);

    /** 事务提交后异步调度AGV队列里排队的下一条任务（当前子任务失败时用），确保先ACK给AGV。 */
    void dispatchNextQueuedAsync(String deviceId);

    /**
     * 兜底轮询：巡检所有仍在等仓储入库阶段的下料计划，对每个计划重新尝试一次
     * "向仓储请求当前波次下一件待入库物料"。
     *
     * <p>正常情况下这是事件驱动的（同波次其它物料入库完成会触发下一件请求），但如果一个
     * 波次里第一件的请求就被仓储拒绝，此后没有任何其它回调会自然触发重试，靠这个Job兜底。
     * 由 {@code AgvReturnWarehouseWindowWatchdog} 调用，跟补料侧 {@code
     * AgvWarehouseWindowWatchdog}/{@link AgvLoadPlanService#pokeActiveWarehousePlans} 同一个模式。
     */
    void pokeActiveWarehousePlans();

    /**
     * {@link #pokeActiveWarehousePlans} 内部对单个计划的重试，独立事务（REQUIRES_NEW），
     * 只供内部通过 self 代理自调用，不直接对外暴露。
     */
    void pokeOneWarehousePlan(String taskId);

    /**
     * 人工强制终止一个卡死的下料计划（管理端手动恢复用）。跟 AGV 子任务物理失败走的是
     * 同一套清理逻辑：状态置 FAILED、取消队列里跟这个计划有关的条目、释放占用的
     * AGV/中转槽位、通知流程 WAIT 节点失败——额外清空 currentAgvTaskId/currentAgvOperation。
     *
     * @return 是否实际执行了终止（false 表示计划不存在或已经是终态，无需处理）
     */
    boolean forceCancel(String taskId, String reason);
}
