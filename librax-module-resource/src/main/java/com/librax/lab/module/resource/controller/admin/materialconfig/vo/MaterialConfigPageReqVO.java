package com.librax.lab.module.resource.controller.admin.materialconfig.vo;

import com.librax.lab.framework.common.pojo.PageParam;
import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;


@Schema(description = "管理后台 - 物料配置分页 Request VO")
@Data
public class MaterialConfigPageReqVO extends PageParam {

    @Schema(description = "类型", example = "1")
    private String type;

    @Schema(description = "类型前缀")
    private String typePrefix;

    @Schema(description = "类型名称", example = "赵六")
    private String typeName;

    @Schema(description = "ID生成规则")
    private String idPattern;

    @Schema(description = "下一个序列号")
    private Integer nextSequence;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "额外字段")
    private String extData;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}