package com.librax.lab.module.resource.controller.admin.rackinfo.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 货架/台面定义，库位的上级容器，归 resource 模块管理分页 Request VO")
@Data
public class RackInfoPageReqVO extends PageParam {

    @Schema(description = "货架唯一编码，如 RACK-A / BENCH-01", example = "28219")
    private String rackId;

    @Schema(description = "货架名称，如 A区样本货架", example = "芋艿")
    private String rackName;

    @Schema(description = "货架类型：RACK=货架 BENCH=操作台 INCUBATOR=孵育箱 FREEZER=冰箱", example = "2")
    private String rackType;

    @Schema(description = "所属区域，关联 lab_zone_quota.zone_code")
    private String zoneCode;

    @Schema(description = "货架行数", example = "25792")
    private Integer rowCount;

    @Schema(description = "货架列数", example = "32624")
    private Integer colCount;

    @Schema(description = "是否启用：1=启用 0=禁用（禁用后下属库位不参与调度）")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}