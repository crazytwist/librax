package com.librax.lab.module.resource.controller.admin.rackinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 货架/台面定义，库位的上级容器，归 resource 模块管理新增/修改 Request VO")
@Data
public class RackInfoSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "16689")
    private Long id;

    @Schema(description = "货架唯一编码，如 RACK-A / BENCH-01", requiredMode = Schema.RequiredMode.REQUIRED, example = "28219")
    @NotEmpty(message = "货架唯一编码，如 RACK-A / BENCH-01不能为空")
    private String rackId;

    @Schema(description = "货架名称，如 A区样本货架", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @NotEmpty(message = "货架名称，如 A区样本货架不能为空")
    private String rackName;

    @Schema(description = "货架类型：RACK=货架 BENCH=操作台 INCUBATOR=孵育箱 FREEZER=冰箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "货架类型：RACK=货架 BENCH=操作台 INCUBATOR=孵育箱 FREEZER=冰箱不能为空")
    private String rackType;

    @Schema(description = "所属区域，关联 lab_zone_quota.zone_code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "所属区域，关联 lab_zone_quota.zone_code不能为空")
    private String zoneCode;

    @Schema(description = "货架原点世界坐标 X（mm），AGV导航基准点")
    private BigDecimal coordX;

    @Schema(description = "货架原点世界坐标 Y（mm）")
    private BigDecimal coordY;

    @Schema(description = "货架原点世界坐标 Z（mm）")
    private BigDecimal coordZ;

    @Schema(description = "货架行数", requiredMode = Schema.RequiredMode.REQUIRED, example = "25792")
    @NotNull(message = "货架行数不能为空")
    private Integer rowCount;

    @Schema(description = "货架列数", requiredMode = Schema.RequiredMode.REQUIRED, example = "32624")
    @NotNull(message = "货架列数不能为空")
    private Integer colCount;

    @Schema(description = "货架层数", requiredMode = Schema.RequiredMode.REQUIRED, example = "20021")
    @NotNull(message = "货架层数不能为空")
    private Integer layerCount;

    @Schema(description = "是否启用：1=启用 0=禁用（禁用后下属库位不参与调度）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否启用：1=启用 0=禁用（禁用后下属库位不参与调度）不能为空")
    private Boolean enabled;

    @Schema(description = "备注说明", example = "你猜")
    private String remark;

}