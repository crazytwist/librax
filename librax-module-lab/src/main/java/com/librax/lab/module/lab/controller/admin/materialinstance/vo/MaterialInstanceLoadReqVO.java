package com.librax.lab.module.lab.controller.admin.materialinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 物料实例上架 Request VO")
@Data
public class MaterialInstanceLoadReqVO {

    @Schema(description = "物料实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "INST-20260501-0001")
    @NotBlank(message = "物料实例ID不能为空")
    private String instanceId;

    @Schema(description = "目标库位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "RACK-A-01")
    @NotBlank(message = "库位编码不能为空")
    private String slotId;

}
