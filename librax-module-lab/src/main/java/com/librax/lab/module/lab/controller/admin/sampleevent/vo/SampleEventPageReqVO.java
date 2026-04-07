package com.librax.lab.module.lab.controller.admin.sampleevent.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]分页 Request VO")
@Data
public class SampleEventPageReqVO extends PageParam {

    @Schema(description = "样本ID", example = "14191")
    private String sampleId;

}