package com.librax.lab.module.resource.controller.admin.resourceconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 资源配置表,运行时锁状态见Redis Response VO")
@Data
@ExcelIgnoreUnannotated
public class ResourceConfigRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "7786")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "资源唯一ID,建议与设备ID一致,如 PH-METER-01、AGV-01", requiredMode = Schema.RequiredMode.REQUIRED, example = "19066")
    @ExcelProperty("资源唯一ID,建议与设备ID一致,如 PH-METER-01、AGV-01")
    private String resourceId;

    @Schema(description = "资源类型,如 PH_METER / AGV / BENCH / TURBIDITY", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("资源类型,如 PH_METER / AGV / BENCH / TURBIDITY")
    private String resourceType;

    @Schema(description = "归属类型:EXCLUSIVE 独占 / SHARED 共享", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("归属类型:EXCLUSIVE 独占 / SHARED 共享")
    private String ownershipType;

    @Schema(description = "独占时必填(归属区域),共享时为空")
    @ExcelProperty("独占时必填(归属区域),共享时为空")
    private String zoneCode;

    @Schema(description = "最大并发持有数,常规为1(同一时刻只能一个流程用),特殊场景可设大", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("最大并发持有数,常规为1(同一时刻只能一个流程用),特殊场景可设大")
    private Integer maxConcurrent;

    @Schema(description = "是否启用,0=禁用不参与调度", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否启用,0=禁用不参与调度")
    private Boolean enabled;

    @Schema(description = "备注", example = "你说的对")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}