package com.librax.lab.module.lab.controller.admin.containertype.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理分页 Request VO")
@Data
public class ContainerTypePageReqVO extends PageParam {

    @Schema(description = "容器类型唯一编码，如 PLATE_96_WELL / TUBE_EP_15ML / BOTTLE_50ML")
    private String typeCode;

    @Schema(description = "容器类型名称，如 96孔板 / 15ml EP管 / 50ml试剂瓶", example = "李四")
    private String typeName;

    @Schema(description = "容器大类：PLATE=孔板 TUBE=试管/离心管 BOTTLE=瓶 RACK=托盘/架 VIAL=小瓶 TIP=吸头 CHIP=芯片 FILTER=滤膜 BOX=盒", example = "2")
    private String containerType;

    @Schema(description = "层级角色：CARRIER=载体（承载其他容器，如托盘/孔板） CONTAINER=直接容器（装内容物，如试管/瓶） WELL=孔（孔板的最小单元）")
    private String hierarchyRole;

    @Schema(description = "最大容积（微升），CARRIER 类型为 NULL")
    private BigDecimal maxVolUl;

    @Schema(description = "是否启用：1=启用 0=停用（停用后不能创建该类型实例）")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}