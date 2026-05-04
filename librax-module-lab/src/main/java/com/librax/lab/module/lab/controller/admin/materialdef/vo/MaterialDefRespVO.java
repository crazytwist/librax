package com.librax.lab.module.lab.controller.admin.materialdef.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MaterialDefRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "7797")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "内容物唯一编码，如 PH-BUFFER-7 / ETHANOL-75PCT", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("内容物唯一编码，如 PH-BUFFER-7 / ETHANOL-75PCT")
    private String materialCode;

    @Schema(description = "内容物名称，如 pH7标准缓冲液 / 75%乙醇", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("内容物名称，如 pH7标准缓冲液 / 75%乙醇")
    private String materialName;

    @Schema(description = "内容物类型：REAGENT=试剂 STANDARD=标准品 BUFFER=缓冲液 SAMPLE=样本 WASTE=废液 MEDIA=培养基 SOLVENT=溶剂", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("内容物类型：REAGENT=试剂 STANDARD=标准品 BUFFER=缓冲液 SAMPLE=样本 WASTE=废液 MEDIA=培养基 SOLVENT=溶剂")
    private String contentType;

    @Schema(description = "计量单位：ml / ul / mg / g / 个", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("计量单位：ml / ul / mg / g / 个")
    private String unit;

    @Schema(description = "供应商名称")
    @ExcelProperty("供应商名称")
    private String supplier;

    @Schema(description = "供应商货号/目录号")
    @ExcelProperty("供应商货号/目录号")
    private String catalogNo;

    @Schema(description = "CAS号，化学物质标识，危险品管控用")
    @ExcelProperty("CAS号，化学物质标识，危险品管控用")
    private String casNo;

    @Schema(description = "标准浓度描述，如 1mol/L / 75% / pH7.0")
    @ExcelProperty("标准浓度描述，如 1mol/L / 75% / pH7.0")
    private String concentration;

    @Schema(description = "存储温度要求，如 2~8℃ / -20℃ / 室温(15~25℃)")
    @ExcelProperty("存储温度要求，如 2~8℃ / -20℃ / 室温(15~25℃)")
    private String storageTemp;

    @Schema(description = "保质期（天），从入库日期计算，NULL表示不限")
    @ExcelProperty("保质期（天），从入库日期计算，NULL表示不限")
    private Integer shelfLifeDays;

    @Schema(description = "开封后有效期（天），开封后重新计算，NULL表示不限")
    @ExcelProperty("开封后有效期（天），开封后重新计算，NULL表示不限")
    private Integer openLifeDays;

    @Schema(description = "危险品等级：NONE=无危险 LOW=低危 MEDIUM=中危 HIGH=高危 FLAMMABLE=易燃 TOXIC=有毒")
    @ExcelProperty("危险品等级：NONE=无危险 LOW=低危 MEDIUM=中危 HIGH=高危 FLAMMABLE=易燃 TOXIC=有毒")
    private String hazardLevel;

    @Schema(description = "其他规格参数 JSON")
    @ExcelProperty("其他规格参数 JSON")
    private String specJson;

    @Schema(description = "是否启用：1=启用 0=停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否启用：1=启用 0=停用")
    private Boolean enabled;

    @Schema(description = "备注说明", example = "随便")
    @ExcelProperty("备注说明")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}