package com.librax.lab.module.resource.controller.app.rackinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "用户 App - 货架 Response VO")
@Data
public class AppRackInfoRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "货架唯一编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "RACK-L")
    private String rackId;

    @Schema(description = "货架名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "载具柜-左侧")
    private String rackName;

    @Schema(description = "货架类型：RACK / BENCH / INCUBATOR / FREEZER", requiredMode = Schema.RequiredMode.REQUIRED, example = "RACK")
    private String rackType;

    @Schema(description = "所属区域", requiredMode = Schema.RequiredMode.REQUIRED)
    private String zoneCode;

    @Schema(description = "货架原点世界坐标 X（mm）")
    private BigDecimal coordX;

    @Schema(description = "货架原点世界坐标 Y（mm）")
    private BigDecimal coordY;

    @Schema(description = "货架原点世界坐标 Z（mm）")
    private BigDecimal coordZ;

    @Schema(description = "货架行数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rowCount;

    @Schema(description = "货架列数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer colCount;

    @Schema(description = "货架层数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer layerCount;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
