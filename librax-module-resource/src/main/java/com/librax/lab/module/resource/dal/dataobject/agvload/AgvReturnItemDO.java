package com.librax.lab.module.resource.dal.dataobject.agvload;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.time.LocalDateTime;

/** AGV 下料计划中的单个物料搬运明细。 */
@TableName("lab_agv_return_item")
@KeySequence("lab_agv_return_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgvReturnItemDO extends BaseDO {

    @TableId
    private Long id;

    private String taskId;
    private Integer sequenceNo;

    /** 卸料波次（由 waveSize 决定，决定 AGV 哪次 UNLOAD 任务处理该物料）。 */
    private Integer waveNo;

    /** 站位库位ID（AGV从此处取料）。 */
    private String sourceSlotId;

    /** AGV 载台槽位ID。 */
    private String agvSlotId;

    /** 仓储中转位ID（AGV将物料卸到此处，等待仓储机械臂取走）。 */
    private String transitSlotId;

    /** 仓储目标货架位置（仓储机械臂将物料放到此处）。 */
    private String warehouseTargetLocation;

    private String instanceId;
    private String containerType;

    /** LOAD 机械臂指令 JSON：从 sourceSlotId 取料到 agvSlotId。 */
    private String step1Json;

    /** MOVE 指令 JSON（仅需一件存储即可，AGV 移动不区分物料）。 */
    private String step2Json;

    /** UNLOAD 机械臂指令 JSON：从 agvSlotId 放料到 transitSlotId。 */
    private String step3Json;

    /** returnMaterials 子请求ID，防止重复请求仓储。 */
    private String warehouseRequestId;

    /**
     * 状态：
     * WAIT_LOAD  - 等待AGV装载
     * ON_AGV     - 已装到AGV上
     * IN_TRANSIT - 已卸到中转位，等待仓储入库
     * RETURNED   - 仓储已确认入库
     * COMPLETED  - commitReturn 后最终完成
     * FAILED     - 出错
     */
    private String status;

    private LocalDateTime dispatchedAt;
    private LocalDateTime loadedAt;
    private LocalDateTime transitAt;
    private LocalDateTime returnedAt;
}
