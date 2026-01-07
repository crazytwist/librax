package com.librax.lab.module.iot.controller.admin.rule.vo.data.sink;

import com.librax.lab.framework.common.enums.CommonStatusEnum;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - IoT 数据流转目的分页 Request VO")
@Data
public class IotDataSinkPageReqVO extends PageParam {

    @Schema(description = "数据目的名称", example = "赵六")
    private String name;

    @Schema(description = "数据目的状态", example = "2")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}