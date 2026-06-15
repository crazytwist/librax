package com.librax.lab.module.lab.controller.app.materialinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "用户 App - 物料实例列表 Request VO")
@Data
public class AppMaterialInstanceListReqVO {

    @Schema(description = "实例唯一ID", example = "INST-20260501-0001")
    private String instanceId;

    @Schema(description = "容器类型编码")
    private String typeCode;

    @Schema(description = "条形码/二维码")
    private String barcode;

    @Schema(description = "当前所在库位ID")
    private String slotId;

    @Schema(description = "当前所在区域")
    private String zoneCode;

    @Schema(description = "内容物类型：REAGENT/STANDARD/BUFFER/SAMPLE/WASTE/EMPTY")
    private String contentType;

    @Schema(description = "内容物编码")
    private String materialCode;

    @Schema(description = "实例状态：AVAILABLE/RESERVED/IN_USE/USED/EXPIRED/DISCARDED")
    private String status;

    @Schema(description = "创建时间（即入库时间）")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
