package com.librax.lab.module.resource.controller.admin.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 物料基础信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MaterialRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "27225")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "编码")
    @ExcelProperty("编码")
    private String code;

    @Schema(description = "名称", example = "王五")
    @ExcelProperty("名称")
    private String name;

    @Schema(description = "类型", example = "2")
    @ExcelProperty("类型")
    private String type;

    @Schema(description = "类别")
    @ExcelProperty("类别")
    private String category;

    @Schema(description = "状态", example = "1")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "区域")
    @ExcelProperty("区域")
    private String area;

    @Schema(description = "位置")
    @ExcelProperty("位置")
    private String location;

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

    @Schema(description = "备注", example = "你说的对")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}