package com.librax.lab.module.device.controller.admin.devicecommand.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_]分页 Request VO")
@Data
public class DeviceCommandPageReqVO extends PageParam {

    @Schema(description = "设备类型，与 lab_device_info.device_type 对应", example = "2")
    private String deviceType;

    @Schema(description = "HTTP请求路径，如 /api/measure，拼接到 base_url 后")
    private String httpPath;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}