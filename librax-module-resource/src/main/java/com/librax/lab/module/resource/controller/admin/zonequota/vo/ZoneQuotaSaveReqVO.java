package com.librax.lab.module.resource.controller.admin.zonequota.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 区域对共享资源的配额新增/修改 Request VO")
@Data
public class ZoneQuotaSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "27704")
    private Long id;

    @Schema(description = "区域编码,如 ZONE-A / ZONE-B", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "区域编码,如 ZONE-A / ZONE-B不能为空")
    private String zoneCode;

    @Schema(description = "共享资源类型,如 AGV", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotEmpty(message = "共享资源类型,如 AGV不能为空")
    private String resourceType;

    @Schema(description = "本区最多同时借用数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "本区最多同时借用数不能为空")
    private Integer maxBorrow;

    @Schema(description = "配额说明", example = "你说的对")
    private String remark;

}