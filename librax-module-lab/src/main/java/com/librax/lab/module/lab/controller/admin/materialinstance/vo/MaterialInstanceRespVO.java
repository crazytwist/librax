package com.librax.lab.module.lab.controller.admin.materialinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 物料实例，库存中每一个具体的容器实例，含层级关系和位置追踪，归 lab 模块管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MaterialInstanceRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "11993")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "实例唯一ID，UUID格式，如 INST-20260501-0001", requiredMode = Schema.RequiredMode.REQUIRED, example = "12596")
    @ExcelProperty("实例唯一ID，UUID格式，如 INST-20260501-0001")
    private String instanceId;

    @Schema(description = "容器类型编码，关联 lab_container_type.type_code，如 PLATE_96_WELL / TUBE_EP_15ML", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("容器类型编码，关联 lab_container_type.type_code，如 PLATE_96_WELL / TUBE_EP_15ML")
    private String typeCode;

    @Schema(description = "条形码/二维码，扫码追踪用，同一系统内唯一")
    @ExcelProperty("条形码/二维码，扫码追踪用，同一系统内唯一")
    private String barcode;

    @Schema(description = "父实例ID，关联同表 instance_id。试管在试管架里时填试管架的instance_id，孔在孔板里填孔板的instance_id，顶层容器为NULL", example = "13141")
    @ExcelProperty("父实例ID，关联同表 instance_id。试管在试管架里时填试管架的instance_id，孔在孔板里填孔板的instance_id，顶层容器为NULL")
    private String parentId;

    @Schema(description = "在父容器中的位置索引，如孔板孔位 A1/B3，试管架位置 01/02，父容器为NULL时此字段也为NULL")
    @ExcelProperty("在父容器中的位置索引，如孔板孔位 A1/B3，试管架位置 01/02，父容器为NULL时此字段也为NULL")
    private String slotIndex;

    @Schema(description = "当前所在库位ID，关联 lab_slot_info.slot_id。顶层容器才有值，子单元（孔）位置跟随父容器，此字段为NULL", example = "10545")
    @ExcelProperty("当前所在库位ID，关联 lab_slot_info.slot_id。顶层容器才有值，子单元（孔）位置跟随父容器，此字段为NULL")
    private String slotId;

    @Schema(description = "当前所在区域，冗余存储便于按区查询，跟随slot_id所在区域")
    @ExcelProperty("当前所在区域，冗余存储便于按区查询，跟随slot_id所在区域")
    private String zoneCode;

    @Schema(description = "内容物类型：REAGENT/STANDARD/BUFFER/SAMPLE/WASTE/EMPTY。EMPTY表示空容器，CARRIER类型容器此字段为NULL", example = "1")
    @ExcelProperty("内容物类型：REAGENT/STANDARD/BUFFER/SAMPLE/WASTE/EMPTY。EMPTY表示空容器，CARRIER类型容器此字段为NULL")
    private String contentType;

    @Schema(description = "内容物编码，关联 lab_material_def.material_code，EMPTY或CARRIER类型为NULL")
    @ExcelProperty("内容物编码，关联 lab_material_def.material_code，EMPTY或CARRIER类型为NULL")
    private String materialCode;

    @Schema(description = "批次号，试剂溯源用，同一批次的试剂 batch_no 相同")
    @ExcelProperty("批次号，试剂溯源用，同一批次的试剂 batch_no 相同")
    private String batchNo;

    @Schema(description = "厂商批号（Lot Number），与 batch_no 区分：batch_no是内部入库批次，lot_no是厂商原始批号")
    @ExcelProperty("厂商批号（Lot Number），与 batch_no 区分：batch_no是内部入库批次，lot_no是厂商原始批号")
    private String lotNo;

    @Schema(description = "当前体积（微升），液体类内容物填写，固体/空容器为NULL，步骤消耗后更新")
    @ExcelProperty("当前体积（微升），液体类内容物填写，固体/空容器为NULL，步骤消耗后更新")
    private BigDecimal currentVolUl;

    @Schema(description = "当前数量（个），固体/耗材类填写，液体类为NULL，步骤消耗后更新")
    @ExcelProperty("当前数量（个）")
    private Integer currentCount;

    @Schema(description = "当前浓度描述，覆盖 material_def 的标准浓度（如稀释后填写实际浓度）")
    @ExcelProperty("当前浓度描述，覆盖 material_def 的标准浓度（如稀释后填写实际浓度）")
    private String concentration;

    @Schema(description = "实例状态：AVAILABLE=可用 RESERVED=已预留（步骤申请但未取用） IN_USE=使用中 USED=已使用完 EXPIRED=已过期 DISCARDED=已废弃", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("实例状态：AVAILABLE=可用 RESERVED=已预留（步骤申请但未取用） IN_USE=使用中 USED=已使用完 EXPIRED=已过期 DISCARDED=已废弃")
    private String status;

    @Schema(description = "入库日期，保质期从此日期计算")
    @ExcelProperty("入库日期，保质期从此日期计算")
    private LocalDate receivedAt;

    @Schema(description = "开封日期，开封后有效期从此日期计算，未开封为NULL")
    @ExcelProperty("开封日期，开封后有效期从此日期计算，未开封为NULL")
    private LocalDate openedAt;

    @Schema(description = "过期日期，由入库日期+保质期天数计算，到期自动标记EXPIRED")
    @ExcelProperty("过期日期，由入库日期+保质期天数计算，到期自动标记EXPIRED")
    private LocalDate expiredAt;

    @Schema(description = "来源流程执行ID，样本类型填写，追踪是哪次实验产生的", example = "17676")
    @ExcelProperty("来源流程执行ID，样本类型填写，追踪是哪次实验产生的")
    private String sourceExecutionId;

    @Schema(description = "来源步骤节点ID，配合 source_execution_id 精确追踪", example = "6951")
    @ExcelProperty("来源步骤节点ID，配合 source_execution_id 精确追踪")
    private String sourceNodeId;

    @Schema(description = "备注说明", example = "你猜")
    @ExcelProperty("备注说明")
    private String remark;

    @Schema(description = "创建时间（即入库时间）", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间（即入库时间）")
    private LocalDateTime createTime;

}