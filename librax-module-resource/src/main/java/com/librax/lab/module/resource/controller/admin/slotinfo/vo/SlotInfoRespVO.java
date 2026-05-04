package com.librax.lab.module.resource.controller.admin.slotinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SlotInfoRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "9454")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "库位唯一编码，同时作为 lab_resource_config.resource_id。固定库位如 RACK-A-01，AGV库位如 AGV-01-SLOT-1", requiredMode = Schema.RequiredMode.REQUIRED, example = "9968")
    @ExcelProperty("库位唯一编码，同时作为 lab_resource_config.resource_id。固定库位如 RACK-A-01，AGV库位如 AGV-01-SLOT-1")
    private String slotId;

    @Schema(description = "库位显示名称，如 A区货架第1槽", example = "赵六")
    @ExcelProperty("库位显示名称，如 A区货架第1槽")
    private String slotName;

    @Schema(description = "库位类型：FIXED=固定台面库位（有世界坐标） AGV=AGV载台槽位（无固定坐标） DEVICE=设备内部位置", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("库位类型：FIXED=固定台面库位（有世界坐标） AGV=AGV载台槽位（无固定坐标） DEVICE=设备内部位置")
    private String slotType;

    @Schema(description = "用途限制：ANY=不限 SAMPLE=样本 REAGENT=试剂 WASTE=废液 BUFFER=缓冲液 TIP=吸头", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("用途限制：ANY=不限 SAMPLE=样本 REAGENT=试剂 WASTE=废液 BUFFER=缓冲液 TIP=吸头")
    private String slotUsage;

    @Schema(description = "所属区域，FIXED和DEVICE类型必填，AGV类型为NULL（跟随AGV移动）")
    @ExcelProperty("所属区域，FIXED和DEVICE类型必填，AGV类型为NULL（跟随AGV移动）")
    private String zoneCode;

    @Schema(description = "所属货架编码，关联 lab_rack_info.rack_id，仅 FIXED 类型填写", example = "11492")
    @ExcelProperty("所属货架编码，关联 lab_rack_info.rack_id，仅 FIXED 类型填写")
    private String rackId;

    @Schema(description = "货架行号（从1开始），便于人工识别位置，仅 FIXED 类型")
    @ExcelProperty("货架行号（从1开始），便于人工识别位置，仅 FIXED 类型")
    private Integer positionRow;

    @Schema(description = "货架列号（从1开始），仅 FIXED 类型")
    @ExcelProperty("货架列号（从1开始），仅 FIXED 类型")
    private Integer positionCol;

    @Schema(description = "货架层号（从1开始），单层货架填1，仅 FIXED 类型")
    @ExcelProperty("货架层号（从1开始），单层货架填1，仅 FIXED 类型")
    private Integer positionLayer;

    @Schema(description = "库位世界坐标 X（mm），AGV导航和机械臂定位用，仅 FIXED 类型")
    @ExcelProperty("库位世界坐标 X（mm），AGV导航和机械臂定位用，仅 FIXED 类型")
    private BigDecimal coordX;

    @Schema(description = "库位世界坐标 Y（mm），仅 FIXED 类型")
    @ExcelProperty("库位世界坐标 Y（mm），仅 FIXED 类型")
    private BigDecimal coordY;

    @Schema(description = "库位世界坐标 Z（mm），仅 FIXED 类型")
    @ExcelProperty("库位世界坐标 Z（mm），仅 FIXED 类型")
    private BigDecimal coordZ;

    @Schema(description = "宿主设备ID，AGV类型填 AGV-01，DEVICE类型填 CENTRIFUGE-01 等，关联 lab_resource_config.resource_id", example = "31947")
    @ExcelProperty("宿主设备ID，AGV类型填 AGV-01，DEVICE类型填 CENTRIFUGE-01 等，关联 lab_resource_config.resource_id")
    private String ownerId;

    @Schema(description = "在宿主设备上的位置编号，AGV类型如 SLOT-1，离心机类型如 POS-1，设备驱动用此字段下发指令")
    @ExcelProperty("在宿主设备上的位置编号，AGV类型如 SLOT-1，离心机类型如 POS-1，设备驱动用此字段下发指令")
    private String localIndex;

    @Schema(description = "最大容纳数量，一般为1（一个库位放一个容器），特殊场景可设大", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("最大容纳数量，一般为1（一个库位放一个容器），特殊场景可设大")
    private Integer capacity;

    @Schema(description = "是否启用：1=启用参与调度 0=禁用（维修/封存时禁用）", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否启用：1=启用参与调度 0=禁用（维修/封存时禁用）")
    private Boolean enabled;

    @Schema(description = "备注，如 靠近B区角落物理遮挡", example = "随便")
    @ExcelProperty("备注，如 靠近B区角落物理遮挡")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}