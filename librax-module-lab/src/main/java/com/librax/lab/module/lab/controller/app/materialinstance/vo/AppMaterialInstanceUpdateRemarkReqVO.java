package com.librax.lab.module.lab.controller.app.materialinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "用户 App - 扫码更新物料实例 remark Request VO")
@Data
public class AppMaterialInstanceUpdateRemarkReqVO {

    @Schema(description = "物料实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "INST-20260501-0001")
    @NotBlank(message = "物料实例ID不能为空")
    private String instId;

    @Schema(description = "合成站指定位置，如 R_A-1-3-7；不传则从 remark JSON 中移除该配置", example = "R_A-1-3-7")
    private String gloveboxTargetSlotId;

    @Schema(description = "是否有盖子；不传则从 remark JSON 中移除该配置", example = "false")
    private Boolean hasCap;

}
