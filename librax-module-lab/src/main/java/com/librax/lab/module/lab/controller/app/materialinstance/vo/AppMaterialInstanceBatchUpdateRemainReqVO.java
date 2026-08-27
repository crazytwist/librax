package com.librax.lab.module.lab.controller.app.materialinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "用户 App - 物料实例批量更新剩余量 Request VO")
@Data
public class AppMaterialInstanceBatchUpdateRemainReqVO {

    @Schema(description = "剩余量更新明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "更新明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "物料实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "INST-20260501-0001")
        @NotBlank(message = "物料实例ID不能为空")
        private String instanceId;

        @Schema(description = "当前数量（个），固体/耗材类填写", example = "10")
        private Integer currentCount;

        @Schema(description = "当前体积（微升），液体类填写", example = "500.00")
        private BigDecimal currentVolUl;

    }

}
