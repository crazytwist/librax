package com.librax.lab.module.resource.controller.admin.materialconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 物料配置新增/修改 Request VO")
@Data
public class MaterialConfigSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "29389")
    private Long id;

    @Schema(description = "类型", example = "1")
    private String type;

    @Schema(description = "类型前缀")
    private String typePrefix;

    @Schema(description = "类型名称", example = "赵六")
    private String typeName;

    @Schema(description = "ID生成规则")
    private String idPattern;

    @Schema(description = "下一个序列号")
    private Integer nextSequence;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "额外字段")
    private String extData;

}