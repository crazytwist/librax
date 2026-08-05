package com.librax.lab.module.resource.dal.dataobject.agvload;

import com.baomidou.mybatisplus.annotation.KeySequence;
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
    private String currentAgvTaskId;
    /** LOAD / MOVE / UNLOAD */
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
    /** LOADING / LOAD_COMPLETE / COMPLETED / FAILED / CANCELLED */
    private String status;
}
