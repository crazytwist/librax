package com.librax.lab.module.flow.api.slot;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库位坐标信息
 * <p>
 * AGV 和机械臂通过世界坐标进行导航定位。
 */
@Data
@Builder
public class SlotCoordinate {

    /** 库位ID */
    private String slotId;

    /** 库位显示名称 */
    private String slotName;

    /** 库位类型: FIXED / AGV / DEVICE */
    private String slotType;

    /** 所属区域 */
    private String zoneCode;

    /** 世界坐标 X (mm) */
    private BigDecimal coordX;

    /** 世界坐标 Y (mm) */
    private BigDecimal coordY;

    /** 世界坐标 Z (mm) */
    private BigDecimal coordZ;

    /** 宿主设备ID（AGV/DEVICE 类型） */
    private String ownerId;

    /** 在宿主设备上的位置编号，设备驱动下发指令用 */
    private String localIndex;
}
