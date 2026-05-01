package com.librax.lab.module.resource.controller.admin.resourceconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 资源配置表,运行时锁状态见Redis新增/修改 Request VO")
@Data
public class ResourceConfigSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "7786")
    private Long id;

    @Schema(description = "资源唯一ID,建议与设备ID一致,如 PH-METER-01、AGV-01", requiredMode = Schema.RequiredMode.REQUIRED, example = "19066")
    @NotEmpty(message = "资源唯一ID,建议与设备ID一致,如 PH-METER-01、AGV-01不能为空")
    private String resourceId;

    @Schema(description = "资源类型,如 PH_METER / AGV / BENCH / TURBIDITY", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "资源类型,如 PH_METER / AGV / BENCH / TURBIDITY不能为空")
    private String resourceType;

    @Schema(description = "归属类型:EXCLUSIVE 独占 / SHARED 共享", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "归属类型:EXCLUSIVE 独占 / SHARED 共享不能为空")
    private String ownershipType;

    @Schema(description = "独占时必填(归属区域),共享时为空")
    private String zoneCode;

    @Schema(description = "最大并发持有数,常规为1(同一时刻只能一个流程用),特殊场景可设大", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "最大并发持有数,常规为1(同一时刻只能一个流程用),特殊场景可设大不能为空")
    private Integer maxConcurrent;

    @Schema(description = "是否启用,0=禁用不参与调度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否启用,0=禁用不参与调度不能为空")
    private Boolean enabled;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

}