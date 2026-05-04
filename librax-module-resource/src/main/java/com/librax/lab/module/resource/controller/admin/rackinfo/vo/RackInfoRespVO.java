package com.librax.lab.module.resource.controller.admin.rackinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 货架/台面定义，库位的上级容器，归 resource 模块管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class RackInfoRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "16689")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "货架唯一编码，如 RACK-A / BENCH-01", requiredMode = Schema.RequiredMode.REQUIRED, example = "28219")
    @ExcelProperty("货架唯一编码，如 RACK-A / BENCH-01")
    private String rackId;

    @Schema(description = "货架名称，如 A区样本货架", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @ExcelProperty("货架名称，如 A区样本货架")
    private String rackName;

    @Schema(description = "货架类型：RACK=货架 BENCH=操作台 INCUBATOR=孵育箱 FREEZER=冰箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("货架类型：RACK=货架 BENCH=操作台 INCUBATOR=孵育箱 FREEZER=冰箱")
    private String rackType;

    @Schema(description = "所属区域，关联 lab_zone_quota.zone_code", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("所属区域，关联 lab_zone_quota.zone_code")
    private String zoneCode;

    @Schema(description = "货架原点世界坐标 X（mm），AGV导航基准点")
    @ExcelProperty("货架原点世界坐标 X（mm），AGV导航基准点")
    private BigDecimal coordX;

    @Schema(description = "货架原点世界坐标 Y（mm）")
    @ExcelProperty("货架原点世界坐标 Y（mm）")
    private BigDecimal coordY;

    @Schema(description = "货架原点世界坐标 Z（mm）")
    @ExcelProperty("货架原点世界坐标 Z（mm）")
    private BigDecimal coordZ;

    @Schema(description = "货架行数", requiredMode = Schema.RequiredMode.REQUIRED, example = "25792")
    @ExcelProperty("货架行数")
    private Integer rowCount;

    @Schema(description = "货架列数", requiredMode = Schema.RequiredMode.REQUIRED, example = "32624")
    @ExcelProperty("货架列数")
    private Integer colCount;

    @Schema(description = "货架层数", requiredMode = Schema.RequiredMode.REQUIRED, example = "20021")
    @ExcelProperty("货架层数")
    private Integer layerCount;

    @Schema(description = "是否启用：1=启用 0=禁用（禁用后下属库位不参与调度）", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否启用：1=启用 0=禁用（禁用后下属库位不参与调度）")
    private Boolean enabled;

    @Schema(description = "备注说明", example = "你猜")
    @ExcelProperty("备注说明")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}