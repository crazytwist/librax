package com.librax.lab.module.lab.controller.admin.containertype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理新增/修改 Request VO")
@Data
public class ContainerTypeSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "7499")
    private Long id;

    @Schema(description = "容器类型唯一编码，如 PLATE_96_WELL / TUBE_EP_15ML / BOTTLE_50ML", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "容器类型唯一编码，如 PLATE_96_WELL / TUBE_EP_15ML / BOTTLE_50ML不能为空")
    private String typeCode;

    @Schema(description = "容器类型名称，如 96孔板 / 15ml EP管 / 50ml试剂瓶", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "容器类型名称，如 96孔板 / 15ml EP管 / 50ml试剂瓶不能为空")
    private String typeName;

    @Schema(description = "容器大类：PLATE=孔板 TUBE=试管/离心管 BOTTLE=瓶 RACK=托盘/架 VIAL=小瓶 TIP=吸头 CHIP=芯片 FILTER=滤膜 BOX=盒", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "容器大类：PLATE=孔板 TUBE=试管/离心管 BOTTLE=瓶 RACK=托盘/架 VIAL=小瓶 TIP=吸头 CHIP=芯片 FILTER=滤膜 BOX=盒不能为空")
    private String containerType;

    @Schema(description = "层级角色：CARRIER=载体（承载其他容器，如托盘/孔板） CONTAINER=直接容器（装内容物，如试管/瓶） WELL=孔（孔板的最小单元）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "层级角色：CARRIER=载体（承载其他容器，如托盘/孔板） CONTAINER=直接容器（装内容物，如试管/瓶） WELL=孔（孔板的最小单元）不能为空")
    private String hierarchyRole;

    @Schema(description = "最大容积（微升），CARRIER 类型为 NULL")
    private BigDecimal maxVolUl;

    @Schema(description = "孔数/位数，仅 CARRIER 类型填写，如 96孔板填 96，24孔试管架填 24", example = "6924")
    private Integer wellCount;

    @Schema(description = "行数，如96孔板=8，24孔架=4，仅 CARRIER 类型")
    private Integer wellRows;

    @Schema(description = "列数，如96孔板=12，24孔架=6，仅 CARRIER 类型")
    private Integer wellCols;

    @Schema(description = "子单元类型编码，CARRIER 类型填写其承载的子容器类型，如孔板→WELL_STANDARD，24孔架→TUBE_EP_15ML")
    private String childTypeCode;

    @Schema(description = "外形尺寸 X（mm），机械臂抓取用")
    private BigDecimal sizeXMm;

    @Schema(description = "外形尺寸 Y（mm）")
    private BigDecimal sizeYMm;

    @Schema(description = "外形尺寸 Z（mm，高度）")
    private BigDecimal sizeZMm;

    @Schema(description = "孔间距（mm），孔板/试管架的相邻位置间距，移液枪多通道操作用")
    private BigDecimal wellSpacingMm;

    @Schema(description = "容器材质，如 PP / PC / GLASS / PS")
    private String material;

    @Schema(description = "其他规格参数 JSON")
    private String specJson;

    @Schema(description = "是否启用：1=启用 0=停用（停用后不能创建该类型实例）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否启用：1=启用 0=停用（停用后不能创建该类型实例）不能为空")
    private Boolean enabled;

    @Schema(description = "备注说明", example = "你猜")
    private String remark;

}