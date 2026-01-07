package com.librax.lab.module.resource.controller.admin.location.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 区位信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class LocationRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "3751")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "区域编码")
    @ExcelProperty("区域编码")
    private String code;

    @Schema(description = "区域名称", example = "王五")
    @ExcelProperty("区域名称")
    private String name;

    @Schema(description = "上级区域", example = "5738")
    @ExcelProperty("上级区域")
    private Long parentId;

    @Schema(description = "层级")
    @ExcelProperty("层级")
    private Integer level;

    @Schema(description = "类型", example = "1")
    @ExcelProperty("类型")
    private String type;

    @Schema(description = "坐标")
    @ExcelProperty("坐标")
    private String coordinates;

    @Schema(description = "状态", example = "1")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "用途")
    @ExcelProperty("用途")
    private String purpose;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "额外字段")
    @ExcelProperty("额外字段")
    private String extData;

    @Schema(description = "拓展字段1")
    @ExcelProperty("拓展字段1")
    private String extField1;

    @Schema(description = "拓展字段2")
    @ExcelProperty("拓展字段2")
    private String extField2;

    @Schema(description = "拓展字段3")
    @ExcelProperty("拓展字段3")
    private String extField3;

    @Schema(description = "拓展字段4")
    @ExcelProperty("拓展字段4")
    private String extField4;

    @Schema(description = "拓展字段5")
    @ExcelProperty("拓展字段5")
    private String extField5;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}