package com.librax.lab.module.lab.controller.admin.sampleresult.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]分页 Request VO")
@Data
public class SampleResultPageReqVO extends PageParam {

    @Schema(description = "样本ID", example = "3524")
    private String sampleId;

    @Schema(description = "检测设备名称，冗余存便于展示", example = "芋艿")
    private String deviceName;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}