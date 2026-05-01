package com.librax.lab.module.resource.controller.admin.stepresourcereq.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 步骤资源需求定义，一个步骤节点可配多行（一步多资源）分页 Request VO")
@Data
public class StepResourceReqPageReqVO extends PageParam {

}