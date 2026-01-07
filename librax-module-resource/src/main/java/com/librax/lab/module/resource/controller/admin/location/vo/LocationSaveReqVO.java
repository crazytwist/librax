package com.librax.lab.module.resource.controller.admin.location.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 区位信息新增/修改 Request VO")
@Data
public class LocationSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "3751")
    private Long id;

    @Schema(description = "区域编码")
    private String code;

    @Schema(description = "区域名称", example = "王五")
    private String name;

    @Schema(description = "上级区域", example = "5738")
    private Long parentId;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "类型", example = "1")
    private String type;

    @Schema(description = "坐标")
    private String coordinates;

    @Schema(description = "状态", example = "1")
    private String status;

    @Schema(description = "用途")
    private String purpose;

    @Schema(description = "备注", example = "你猜")
    private String remark;

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

}