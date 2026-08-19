package com.librax.lab.module.resource.dal.dataobject.agvload;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AGV 编排请求队列。
 *
 * <p>跟 {@link AgvTaskQueueDO}（单条 startTask 指令级别的排队）是不同层级：这张表排的是
 * "整次编排请求"（startWarehouseOrchestration / startReturnOrchestration 的一次调用），
 * 在预留 AGV 槽位/中转位/目标库位时如果撞上占用冲突（AGV_SLOT_UNAVAILABLE），
 * 不直接让流程节点失败，而是把原始请求存进这张表，槽位释放时再重新尝试。
 *
 * <p>status 流转：PENDING → DISPATCHED（重试成功，编排已真正开始）
 *                PENDING → CANCELLED（对应流程执行已不在 RUNNING，放弃重试）
 */
@TableName("lab_agv_orchestrate_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgvOrchestrateQueueDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** AGV 设备ID，与 lab_agv_load_plan / lab_agv_return_plan.device_id 一致。 */
    private String deviceId;

    /** 计划类型：LOAD（补料/机台转运）/ RETURN（下料）。 */
    private String planType;

    /** 流程执行ID（即 plan.taskId），全局唯一，一次执行只应该有一条排队记录。 */
    private String executionId;

    /** 流程编排等待节点ID（orchestrationNodeId），重试成功后建 plan 要用。 */
    private String nodeId;

    /** 原始编排入参 JSON：{"items": [...], "inputParams": {...}}。 */
    private String payload;

    /** PENDING / DISPATCHED / CANCELLED */
    private String status;

    /** 最近一次重试失败原因，仅日志/排查用途，不影响是否继续重试。 */
    private String errorMessage;

    private LocalDateTime createTime;
    private LocalDateTime dispatchTime;
}
