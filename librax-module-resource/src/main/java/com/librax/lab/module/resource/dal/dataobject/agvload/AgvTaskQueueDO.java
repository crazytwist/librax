package com.librax.lab.module.resource.dal.dataobject.agvload;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AGV 任务队列。
 *
 * <p>单台 AGV 同一时刻只能执行一个 startTask。
 * 仓储备料完成后若 AGV 繁忙，任务以 PENDING 入队；
 * 当前任务完成（taskState runState=2）后自动调度队首 PENDING 任务。
 *
 * <p>status 流转：PENDING → DISPATCHED → DONE
 *                PENDING / DISPATCHED → CANCELLED（失败或计划取消时）
 */
@TableName("lab_agv_task_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgvTaskQueueDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** AGV 设备ID，与 lab_agv_load_plan.device_id 一致。 */
    private String deviceId;

    /** 对应的 AgvLoadPlan.taskId（即流程 executionId）。 */
    private String planTaskId;

    /** 下发给 AGV 的任务ID（如 executionId-LOAD-1、executionId-MOVE）。 */
    private String agvTaskId;

    /** 操作类型：LOAD / MOVE / UNLOAD。 */
    private String operation;

    /** 计划类型：LOAD（补料）/ RETURN（下料）。 */
    private String planType;

    /** startTask 请求体 JSON（含 taskId、agvCmdList）。 */
    private String payload;

    /** PENDING / DISPATCHED / DONE / CANCELLED */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime dispatchTime;
}
