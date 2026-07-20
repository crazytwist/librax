package com.librax.lab.module.resource.dal.dataobject.agvload;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * AGV 下料计划。
 *
 * <p>流程：AGV一次装载全部物料 → 移动到仓储区 → 按波次卸料到中转位 → 仓储机械臂逐件入库。
 * 波次上限由 {@code waveSize}（中转位容量）决定，完成一波再卸下一波。
 */
@TableName("lab_agv_return_plan")
@KeySequence("lab_agv_return_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgvReturnPlanDO extends BaseDO {

    @TableId
    private Long id;

    /** 流程执行ID（pipeline executionId），作为全局唯一任务标识。 */
    private String taskId;

    /** AGV 设备ID。 */
    private String deviceId;

    /** 计划下料物料总数。 */
    private Integer expectedCount;

    /** 仓储已确认入库的物料数量。 */
    private Integer returnedCount;

    /** 每波次中转位容量（仓储一次最多处理几件）。 */
    private Integer waveSize;

    /** 当前卸料波次（1-based）。 */
    private Integer currentWave;

    /** 总卸料波次。 */
    private Integer totalWaves;

    /** 当前正在执行的 AGV 子任务ID。 */
    private String currentAgvTaskId;

    /** 当前 AGV 操作类型：LOAD / MOVE / UNLOAD。 */
    private String currentAgvOperation;

    /** AGV 最后上报的站点信息。 */
    private String lastAgvStation;

    /** 流程编排等待节点ID，用于 stepCallbackSpi 唤醒流程。 */
    private String orchestrationNodeId;

    /** 仓储入库回调 URL（returnMaterials callbackUrl）。 */
    private String warehouseCallbackUrl;

    /** AGV 任务类型。 */
    private String taskType;

    /** AGV 托板类型。 */
    private String plateType;

    /**
     * 状态流转：
     * WAIT_AGV_LOAD → WAIT_AGV_MOVE → WAIT_AGV_UNLOAD → WAIT_WAREHOUSE
     * （每波 UNLOAD 后循环 WAIT_AGV_UNLOAD → WAIT_WAREHOUSE，直到全部波次完成）
     * → WAIT_FLOW_COMMIT → COMPLETED
     * 任意状态可跳转 → FAILED
     * AGV繁忙时：QUEUED（排队中）
     * AGV等待现场动作：WAIT_SITE_ACTION
     */
    private String status;
}
