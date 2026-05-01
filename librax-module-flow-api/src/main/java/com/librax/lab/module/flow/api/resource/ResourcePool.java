package com.librax.lab.module.flow.api.resource;

/**
 * 资源池 SPI
 * <p>
 * resource 模块实现,flow / device / task 模块通过此接口申请与释放资源。
 * <p>
 * 关注点:运行时的"能不能占用",不关心设备型号、健康状态、库存数量等长期属性。
 * 资源类型(如 PH_METER / AGV)由调用方指定,resource 模块按配置决定具体分配哪一台。
 *
 * <p>典型调用流程:
 * <pre>
 *   1. StepSubmitter.submit 前调 acquire,拿到 resourceId
 *   2. 把 resourceId 塞进 StepDispatchContext,执行器用它发设备指令
 *   3. 回调 / 失败 / 超时时调 release(或 releaseByHolder 批量释放)
 * </pre>
 */
public interface ResourcePool {

    /**
     * 申请资源
     *
     * @param request 申请请求
     * @return 申请结果,成功时携带 resourceId,失败时携带原因和建议重试间隔
     */
    AcquireResult acquire(AcquireRequest request);

    /**
     * 释放单个资源
     * <p>
     * 校验持有者匹配才释放,防止并发场景下 A 误释放 B 的锁。
     * 持有者不匹配时静默忽略(打日志),不抛异常。
     *
     * @param resourceId acquire 返回的资源 ID
     * @param holderKey  持有者标识,必须和 acquire 时一致
     */
    void release(String resourceId, String holderKey);

    /**
     * 按持有者批量释放
     * <p>
     * 用于流程终止、步骤超时兜底等场景,一次性释放该持有者占用的全部资源。
     * 比如 executionId=X 的流程 FAILED 时,调 releaseByHolder("X:*") 清理残留。
     *
     * @param holderKey 持有者标识
     * @return 实际释放的资源数量
     */
    int releaseByHolder(String holderKey);

    /**
     * 申请成功后写占用记录，由 resource 模块实现
     *
     * @param executionId  流程ID
     * @param nodeId       节点ID
     * @param attempt      尝试次数
     * @param resourceId   资源ID
     * @param resourceType 资源类型
     */
    void recordHold(String executionId, String nodeId, int attempt,
                    String resourceId, String resourceType);


    /**
     * 标记资源占用记录为已释放（pe_step_resource_hold.released_at）
     * <p>
     * 和 release/releaseByHolder 配套，由 flow 的释放方法调用。
     *
     * @param executionId 流程执行 ID
     * @param nodeId      节点 ID
     * @param attempt     当前重试次数
     * @param reason      释放原因（STEP_COMPLETE/TIMEOUT/STEP_DEAD/RECOVERY）
     */
    void markHoldReleased(String executionId, String nodeId,
                          int attempt, String reason);

    /**
     * 查当前持有记录（宕机恢复用）
     *
     * @param executionId 流程ID
     * @param nodeId      节点ID
     * @param attempt     尝试次数
     * @return 资源ID
     */
    String queryHeldResourceId(String executionId, String nodeId, int attempt);
}