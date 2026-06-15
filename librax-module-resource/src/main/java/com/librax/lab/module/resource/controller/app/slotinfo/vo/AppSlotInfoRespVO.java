package com.librax.lab.module.resource.controller.app.slotinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "用户 App - 库位 Response VO")
@Data
public class AppSlotInfoRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "库位唯一编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "RACK-L-R1-C1")
    private String slotId;

    @Schema(description = "库位显示名称", example = "载具柜-左侧 色谱瓶 R1C1")
    private String slotName;

    @Schema(description = "库位类型：FIXED / AGV / DEVICE", requiredMode = Schema.RequiredMode.REQUIRED, example = "FIXED")
    private String slotType;

    @Schema(description = "用途限制：ANY / SAMPLE / REAGENT / WASTE / BUFFER / TIP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String slotUsage;

    @Schema(description = "所属区域")
    private String zoneCode;

    @Schema(description = "所属货架编码", example = "RACK-L")
    private String rackId;

    @Schema(description = "货架行号（从1开始）")
    private Integer positionRow;

    @Schema(description = "货架列号（从1开始）")
    private Integer positionCol;

    @Schema(description = "货架层号（从1开始）")
    private Integer positionLayer;

    @Schema(description = "库位世界坐标 X（mm）")
    private BigDecimal coordX;

    @Schema(description = "库位世界坐标 Y（mm）")
    private BigDecimal coordY;

    @Schema(description = "库位世界坐标 Z（mm）")
    private BigDecimal coordZ;

    @Schema(description = "宿主设备ID")
    private String ownerId;

    @Schema(description = "在宿主设备上的位置编号")
    private String localIndex;

    @Schema(description = "最大容纳数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer capacity;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
