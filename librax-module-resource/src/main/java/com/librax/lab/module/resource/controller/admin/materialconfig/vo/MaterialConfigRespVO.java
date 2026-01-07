package com.librax.lab.module.resource.controller.admin.materialconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 物料配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MaterialConfigRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "29389")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "类型", example = "1")
    @ExcelProperty("类型")
    private String type;

    @Schema(description = "类型前缀")
    @ExcelProperty("类型前缀")
    private String typePrefix;

    @Schema(description = "类型名称", example = "赵六")
    @ExcelProperty("类型名称")
    private String typeName;

    @Schema(description = "ID生成规则")
    @ExcelProperty("ID生成规则")
    private String idPattern;

    @Schema(description = "下一个序列号")
    @ExcelProperty("下一个序列号")
    private Integer nextSequence;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "额外字段")
    @ExcelProperty("额外字段")
    private String extData;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}