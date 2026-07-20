package com.librax.lab.module.resource.dal.dataobject.agvload;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.time.LocalDateTime;

/** AGV 装载计划中的单个物料位置绑定。 */
@TableName("lab_agv_load_item")
@KeySequence("lab_agv_load_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgvLoadItemDO extends BaseDO {

    @TableId
    private Long id;
    private String taskId;
    private Integer sequenceNo;
    private String sourceSlotId;
    private String agvSlotId;
    private String targetSlotId;
    private String instanceId;
    private Integer waveNo;
    private String warehouseSourceLocation;
    private String step1Json;
    private String step2Json;
    private String step3Json;
    /** WAIT_SOURCE / READY / DISPATCHED / LOADED / COMPLETED */
    private String status;
    private String containerType;
    private String warehouseRequestId;
    private String readyRequestId;
    private LocalDateTime readyAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime loadedAt;
}
