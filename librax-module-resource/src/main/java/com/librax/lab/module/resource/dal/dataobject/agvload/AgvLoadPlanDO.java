package com.librax.lab.module.resource.dal.dataobject.agvload;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/** 一次 AGV 车次内的多轮装载计划。 */
@TableName("lab_agv_load_plan")
@KeySequence("lab_agv_load_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgvLoadPlanDO extends BaseDO {

    @TableId
    private Long id;
    private String taskId;
    private String deviceId;
    private String loadStation;
    private Integer expectedCount;
    private Integer loadedCount;
    private Integer waveSize;
    private Integer currentWave;
    private Boolean allowPartialLoad;
    private Boolean agvWaiting;
    private String lastAgvStation;
    private String orchestrationNodeId;
    /**
     * MyBatis-Plus updateById() 默认按 NOT_NULL 策略生成UPDATE：Java对象里这个字段是null，
     * 生成的SQL就完全不包含这一列，而不是把它UPDATE成NULL——代码里多处会
     * plan.setCurrentAgvTaskId(null) 来表示"AGV当前空闲、没有子任务在途"，如果不强制
     * ALWAYS策略，这个null永远不会真正落到数据库里，导致这一列长期停留在上一次派发时的
     * 值，后续任何重新从DB读取plan的地方（哪怕加了行锁）都会看到"AGV还在忙"的假象，
     * 从而漏发下一条AGV指令——这是一个真实出现过的生产问题，务必保留这个注解。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String currentAgvTaskId;
    /** PREPOSITION / LOAD / MOVE / UNLOAD。同上，必须ALWAYS策略，否则清空后无法真正落库。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String currentAgvOperation;
    private String taskType;
    private String plateType;
    private String warehouseCallbackUrl;
    /**
     * 每波物料如何变为"就绪"：
     * WAREHOUSE（默认）— 逐件下发 prepareMaterials，等仓储 slot/ready 回调；
     * DIRECT — 物料本就在源库位上（如机台间转运），跳过仓储环节直接标记就绪。
     */
    private String sourceReadyMode;
    /** 卸货前是否需要开关门 */
    private Boolean doorRequired;
    /** 门禁设备类型，留空默认 Feeding-Reaction */
    private String doorDeviceId;
    /** 开关门指令码，留空默认 OpenCloseDoor */
    private String doorCommandCode;
    /** 机台"可以开门"确认信号是否已收到 */
    private Boolean doorPermissionReceived;
    /** 是否需要额外等机台开门许可信号（同单第一车=0，第二车起=1） */
    private Boolean doorPermissionGateRequired;
    /** 当前车次合成站门是否已成功打开，用于提前开门及UNLOAD兜底幂等 */
    private Boolean doorOpened;
    /** WAIT_WAREHOUSE / WAIT_WAREHOUSE_PREPOSITIONED / WAIT_AGV_* / COMPLETED / FAILED / CANCELLED */
    private String status;
}
