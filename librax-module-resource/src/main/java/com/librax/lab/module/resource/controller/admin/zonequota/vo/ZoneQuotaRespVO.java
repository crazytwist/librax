package com.librax.lab.module.resource.controller.admin.zonequota.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 区域对共享资源的配额 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ZoneQuotaRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "27704")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "区域编码,如 ZONE-A / ZONE-B", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("区域编码,如 ZONE-A / ZONE-B")
    private String zoneCode;

    @Schema(description = "共享资源类型,如 AGV", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("共享资源类型,如 AGV")
    private String resourceType;

    @Schema(description = "本区最多同时借用数", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("本区最多同时借用数")
    private Integer maxBorrow;

    @Schema(description = "配额说明", example = "你说的对")
    @ExcelProperty("配额说明")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}