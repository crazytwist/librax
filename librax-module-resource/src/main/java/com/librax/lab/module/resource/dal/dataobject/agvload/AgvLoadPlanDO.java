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
    /** LOADING / LOAD_COMPLETE / COMPLETED / FAILED / CANCELLED */
    private String status;
}
