package com.librax.lab.module.lab.controller.app.materialinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "用户 App - 物料实例 Response VO")
@Data
public class AppMaterialInstanceRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "实例唯一ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "INST-20260501-0001")
    private String instanceId;

    @Schema(description = "容器类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String typeCode;

    @Schema(description = "条形码/二维码")
    private String barcode;

    @Schema(description = "父实例ID")
    private String parentId;

    @Schema(description = "在父容器中的位置索引")
    private String slotIndex;

    @Schema(description = "当前所在库位ID")
    private String slotId;

    @Schema(description = "当前所在区域")
    private String zoneCode;

    @Schema(description = "内容物类型：REAGENT/STANDARD/BUFFER/SAMPLE/WASTE/EMPTY")
    private String contentType;

    @Schema(description = "内容物编码")
    private String materialCode;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "厂商批号")
    private String lotNo;

    @Schema(description = "当前体积（微升）")
    private BigDecimal currentVolUl;

    @Schema(description = "当前数量（个）")
    private Integer currentCount;

    @Schema(description = "当前浓度描述")
    private String concentration;

    @Schema(description = "实例状态：AVAILABLE/RESERVED/IN_USE/USED/EXPIRED/DISCARDED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "入库日期")
    private LocalDate receivedAt;

    @Schema(description = "开封日期")
    private LocalDate openedAt;

    @Schema(description = "过期日期")
    private LocalDate expiredAt;

    @Schema(description = "来源流程执行ID")
    private String sourceExecutionId;

    @Schema(description = "来源步骤节点ID")
    private String sourceNodeId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间（即入库时间）", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
