package com.librax.lab.module.lab.controller.admin.materialconsumption.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MaterialConsumptionRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "29631")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "流程执行ID，关联 pe_pipeline_execution.execution_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "14384")
    @ExcelProperty("流程执行ID，关联 pe_pipeline_execution.execution_id")
    private String executionId;

    @Schema(description = "步骤节点ID，关联 pd_pipeline_step.node_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "9896")
    @ExcelProperty("步骤节点ID，关联 pd_pipeline_step.node_id")
    private String nodeId;

    @Schema(description = "第几次重试，与 pe_step_execution.attempt 对应", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("第几次重试，与 pe_step_execution.attempt 对应")
    private Integer attempt;

    @Schema(description = "操作的物料实例ID，关联 lab_material_instance.instance_id", requiredMode = Schema.RequiredMode.REQUIRED, example = "9294")
    @ExcelProperty("操作的物料实例ID，关联 lab_material_instance.instance_id")
    private String instanceId;

    @Schema(description = "容器类型编码，冗余存储便于统计")
    @ExcelProperty("容器类型编码，冗余存储便于统计")
    private String typeCode;

    @Schema(description = "内容物编码，冗余存储便于溯源")
    @ExcelProperty("内容物编码，冗余存储便于溯源")
    private String materialCode;

    @Schema(description = "批次号，冗余存储便于批次追踪")
    @ExcelProperty("批次号，冗余存储便于批次追踪")
    private String batchNo;

    @Schema(description = "操作类型：CONSUME=消耗（体积减少） TRANSFER=转移（位置变化） PRODUCE=产生（步骤输出新物料） DISCARD=废弃 RESERVE=预留 RELEASE=释放预留", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("操作类型：CONSUME=消耗（体积减少） TRANSFER=转移（位置变化） PRODUCE=产生（步骤输出新物料） DISCARD=废弃 RESERVE=预留 RELEASE=释放预留")
    private String action;

    @Schema(description = "操作前体积（微升），液体类填写")
    @ExcelProperty("操作前体积（微升），液体类填写")
    private BigDecimal volBeforeUl;

    @Schema(description = "体积变化量（微升），消耗为负值如-100，产生为正值如+200，转移为0")
    @ExcelProperty("体积变化量（微升），消耗为负值如-100，产生为正值如+200，转移为0")
    private BigDecimal volChangeUl;

    @Schema(description = "操作后体积（微升），= vol_before_ul + vol_change_ul")
    @ExcelProperty("操作后体积（微升），= vol_before_ul + vol_change_ul")
    private BigDecimal volAfterUl;

    @Schema(description = "转移来源库位，TRANSFER操作时填写", example = "2853")
    @ExcelProperty("转移来源库位，TRANSFER操作时填写")
    private String fromSlotId;

    @Schema(description = "转移目标库位，TRANSFER操作时填写", example = "30485")
    @ExcelProperty("转移目标库位，TRANSFER操作时填写")
    private String toSlotId;

    @Schema(description = "操作发生时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("操作发生时间")
    private LocalDateTime consumedAt;

    @Schema(description = "备注说明", example = "你猜")
    @ExcelProperty("备注说明")
    private String remark;

    @Schema(description = "记录创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("记录创建时间")
    private LocalDateTime createTime;

}