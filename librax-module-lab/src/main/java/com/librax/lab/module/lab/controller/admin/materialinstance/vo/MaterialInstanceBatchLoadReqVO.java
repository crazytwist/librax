package com.librax.lab.module.lab.controller.admin.materialinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 物料实例批量上架 Request VO")
@Data
public class MaterialInstanceBatchLoadReqVO {

    @Schema(description = "上架明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "上架明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "物料实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "INST-20260501-0001")
        @NotBlank(message = "物料实例ID不能为空")
        private String instanceId;

        @Schema(description = "目标库位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "RACK-A-01")
        @NotBlank(message = "库位编码不能为空")
        private String slotId;

    }

}
