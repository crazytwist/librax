package com.librax.lab.module.resource.controller.admin.material.vo;

import com.librax.lab.framework.common.pojo.PageParam;
import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;


@Schema(description = "管理后台 - 物料基础信息分页 Request VO")
@Data
public class MaterialPageReqVO extends PageParam {

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称", example = "王五")
    private String name;

    @Schema(description = "类型", example = "2")
    private String type;

    @Schema(description = "类别")
    private String category;

    @Schema(description = "状态", example = "1")
    private String status;

    @Schema(description = "区域")
    private String area;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "额外字段")
    private String extData;

    @Schema(description = "拓展字段1")
    private String extField1;

    @Schema(description = "拓展字段2")
    private String extField2;

    @Schema(description = "拓展字段3")
    private String extField3;

    @Schema(description = "拓展字段4")
    private String extField4;

    @Schema(description = "拓展字段5")
    private String extField5;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}