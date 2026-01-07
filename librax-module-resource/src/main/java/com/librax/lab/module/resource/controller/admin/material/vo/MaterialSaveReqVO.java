package com.librax.lab.module.resource.controller.admin.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 物料基础信息新增/修改 Request VO")
@Data
public class MaterialSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "27225")
    private Long id;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称", example = "王五")
    private String name;

    @Schema(description = "类型", example = "2")
    private String type;

    @Schema(description = "类别")
    private String category;

    @Schema(description = "状态", example = "1")
    private String status;

    @Schema(description = "区域")
    private String area;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "额外字段")
    private String extData;

    @Schema(description = "拓展字段1")
    private String extField1;

    @Schema(description = "拓展字段2")
    private String extField2;

    @Schema(description = "拓展字段3")
    private String extField3;

    @Schema(description = "拓展字段4")
    private String extField4;

    @Schema(description = "拓展字段5")
    private String extField5;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

}