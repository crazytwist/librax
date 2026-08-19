package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.module.resource.controller.app.slotinfo.vo.WarehouseCallbackReqVO;

import java.util.List;
import java.util.Map;

/** AGV 一车任务内多轮装载的状态协调服务。 */
public interface AgvLoadPlanService {

    void registerIfEnabled(String taskId,
                           List<Map<String, Object>> transferItems,
                           Map<String, Object> inputParams);

    void startWarehouseOrchestration(String taskId,
                                     String nodeId,
                                     List<Map<String, Object>> transferItems,
                                     Map<String, Object> inputParams);

    /**
     * {@link #startWarehouseOrchestration} 的实际尝试实现，独立事务（REQUIRES_NEW）：
     * 抢占AGV/目标槽位失败时抛出的异常只回滚这一次尝试，不影响调用方（外层方法本身，
     * 或者排队重试）所在的事务。仅供 {@link #startWarehouseOrchestration} 和
     * {@link #tryDispatchQueuedOrchestrationAsync} 内部调用，不直接对外暴露。
     */
    void attemptStartWarehouseOrchestration(String taskId,
                                            String nodeId,
                                            List<Map<String, Object>> transferItems,
                                            Map<String, Object> inputParams);

    /**
     * 事务提交后异步尝试从 {@code lab_agv_orchestrate_queue} 里取出最早一条排队的补料编排请求重试。
     * 由所有会释放 AGV/目标槽位的地方（如 {@link #commitTransfer}）触发。
     */
    void tryDispatchQueuedOrchestrationAsync(String deviceId);

    Map<String, Object> markSlotReady(WarehouseCallbackReqVO req);

    Map<String, Object> getProgress(String taskId);

    boolean hasPlan(String taskId);

    void commitTransfer(String taskId);

    void triggerNextStepAsync(String taskId);

    /** 事务提交后异步执行门禁联动检查（开门+放行AGV），避免阻塞 waitSignal 的HTTP响应。 */
    void maybeGateOnDoorOrResumeAsync(String taskId);

    /**
     * 事务提交后异步推进AGV分阶段任务的下一步（LOAD->MOVE / MOVE->UNLOAD / UNLOAD->完成），
     * 确保 taskState() 先把ACK返回给AGV，再发下一条 startTask 指令。
     */
    void advanceAfterTaskStateAsync(String taskId);

    /** 事务提交后异步调度AGV队列里排队的下一条任务（当前子任务失败时用），确保先ACK给AGV。 */
    void dispatchNextQueuedAsync(String deviceId);

    /**
     * 兜底轮询：巡检所有仍在物料收集阶段（WAREHOUSE逐件模式）的计划，
     * 对每个计划重新尝试一次"派发已就绪物料给AGV / 向仓储窗口补位"。
     *
     * <p>正常情况下这两个动作都是事件驱动的（某件物料就绪、被AGV抓走都会触发），
     * 但仓储请求现在改成了失败不重试（见 doRequestWarehouseItem 的说明），
     * 极端情况下（比如恰好是本计划最后一件请求失败，之后再没有别的事件会触发它）
     * 可能永久卡住，靠这个定时Job兜底。由 {@code AgvWarehouseWindowWatchdog} 调用。
     */
    void pokeActiveWarehousePlans();

    /**
     * {@link #pokeActiveWarehousePlans} 内部对单个计划的重试，独立事务（REQUIRES_NEW），
     * 只供内部通过 self 代理自调用，不直接对外暴露。
     */
    void pokeOneWarehousePlan(String taskId);

    /**
     * 人工强制终止一个卡死的补料计划（管理端手动恢复用）。跟 AGV 子任务物理失败走的是
     * 同一套清理逻辑：状态置 FAILED、取消队列里跟这个计划有关的条目、释放占用的
     * AGV/目标槽位、通知流程 WAIT 节点失败——额外清空 currentAgvTaskId/currentAgvOperation。
     *
     * @return 是否实际执行了终止（false 表示计划不存在或已经是终态，无需处理）
     */
    boolean forceCancel(String taskId, String reason);
}
