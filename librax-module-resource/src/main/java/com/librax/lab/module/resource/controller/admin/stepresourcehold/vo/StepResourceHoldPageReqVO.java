package com.librax.lab.module.resource.controller.admin.stepresourcehold.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 步骤执行资源占用记录，released_at IS NULL 表示当前持有中分页 Request VO")
@Data
public class StepResourceHoldPageReqVO extends PageParam {

}