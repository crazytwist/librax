package com.librax.lab.module.resource.controller.admin.zonequota.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 区域对共享资源的配额分页 Request VO")
@Data
public class ZoneQuotaPageReqVO extends PageParam {

    @Schema(description = "区域编码,如 ZONE-A / ZONE-B")
    private String zoneCode;

    @Schema(description = "共享资源类型,如 AGV", example = "1")
    private String resourceType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}