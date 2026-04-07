package com.librax.lab.module.lab.controller.admin.sampleinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SampleInfoRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "30878")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "样本唯一业务ID（条码/二维码）", requiredMode = Schema.RequiredMode.REQUIRED, example = "22328")
    @ExcelProperty("样本唯一业务ID（条码/二维码）")
    private String sampleId;

    @Schema(description = "父样本ID，NULL表示原始样本", example = "11018")
    @ExcelProperty("父样本ID，NULL表示原始样本")
    private String parentSampleId;

    @Schema(description = "根样本ID，冗余存便于查整棵谱系树，原始样本此字段=sample_id", example = "12154")
    @ExcelProperty("根样本ID，冗余存便于查整棵谱系树，原始样本此字段=sample_id")
    private String rootSampleId;

    @Schema(description = "衍生类型：ORIGINAL原始 / SPLIT拆分 / MERGE合并 / ALIQUOT分装 / ADD_REAGENT加试剂", example = "2")
    @ExcelProperty("衍生类型：ORIGINAL原始 / SPLIT拆分 / MERGE合并 / ALIQUOT分装 / ADD_REAGENT加试剂")
    private String deriveType;

    @Schema(description = "谱系代数，原始样本=0，每拆分/合并一次+1")
    @ExcelProperty("谱系代数，原始样本=0，每拆分/合并一次+1")
    private Integer generation;

    @Schema(description = "样本类型：BLOOD/URINE/TISSUE/WATER/SOIL/AIR/MIXTURE", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("样本类型：BLOOD/URINE/TISSUE/WATER/SOIL/AIR/MIXTURE")
    private String sampleType;

    @Schema(description = "样本名称/描述", example = "芋艿")
    @ExcelProperty("样本名称/描述")
    private String sampleName;

    @Schema(description = "容器类型：TUBE试管/PLATE微孔板/VIAL样品瓶/BAG采样袋", example = "2")
    @ExcelProperty("容器类型：TUBE试管/PLATE微孔板/VIAL样品瓶/BAG采样袋")
    private String containerType;

    @Schema(description = "容器条码")
    @ExcelProperty("容器条码")
    private String containerCode;

    @Schema(description = "当前体积(微升)")
    @ExcelProperty("当前体积(微升)")
    private BigDecimal volumeUl;

    @Schema(description = "初始体积(微升)，登记时记录，不再变更")
    @ExcelProperty("初始体积(微升)，登记时记录，不再变更")
    private BigDecimal initialVolumeUl;

    @Schema(description = "浓度(mg/mL)，部分样本需要")
    @ExcelProperty("浓度(mg/mL)，部分样本需要")
    private BigDecimal concentration;

    @Schema(description = "扩展属性，不同类型样本有不同属性")
    @ExcelProperty("扩展属性，不同类型样本有不同属性")
    private String attributes;

    @Schema(description = "REGISTERED/LOADED/IN_PROCESS/SPLIT/COMPLETED/ARCHIVED/REJECTED/LOST", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("REGISTERED/LOADED/IN_PROCESS/SPLIT/COMPLETED/ARCHIVED/REJECTED/LOST")
    private String status;

    @Schema(description = "当前物理位置（架位编号/工位编号/冷库编号）")
    @ExcelProperty("当前物理位置（架位编号/工位编号/冷库编号）")
    private String locationCode;

    @Schema(description = "位置详情（如冷库的具体层架位：R2-S3-P5）")
    @ExcelProperty("位置详情（如冷库的具体层架位：R2-S3-P5）")
    private String locationDetail;

    @Schema(description = "当前正在执行的流程ID", example = "8800")
    @ExcelProperty("当前正在执行的流程ID")
    private String currentExecutionId;

    @Schema(description = "当前所在步骤节点ID", example = "5139")
    @ExcelProperty("当前所在步骤节点ID")
    private String currentNodeId;

    @Schema(description = "样本批次号，同一批送检的样本共享")
    @ExcelProperty("样本批次号，同一批送检的样本共享")
    private String batchNo;

    @Schema(description = "检测单号/委托单号")
    @ExcelProperty("检测单号/委托单号")
    private String orderNo;

    @Schema(description = "优先级 0普通 1加急 2特急，影响资源调度排队顺序")
    @ExcelProperty("优先级 0普通 1加急 2特急，影响资源调度排队顺序")
    private Integer priority;

    @Schema(description = "来源：MANUAL手动/IMPORT导入/LIS系统对接/DEVICE设备采集")
    @ExcelProperty("来源：MANUAL手动/IMPORT导入/LIS系统对接/DEVICE设备采集")
    private String source;

    @Schema(description = "外部系统ID（LIS单号/HIS医嘱号等）", example = "21433")
    @ExcelProperty("外部系统ID（LIS单号/HIS医嘱号等）")
    private String externalId;

    @Schema(description = "采样时间")
    @ExcelProperty("采样时间")
    private LocalDateTime collectedAt;

    @Schema(description = "收样时间（到达实验室）")
    @ExcelProperty("收样时间（到达实验室）")
    private LocalDateTime receivedAt;

    @Schema(description = "样本有效期")
    @ExcelProperty("样本有效期")
    private LocalDateTime expireTime;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}