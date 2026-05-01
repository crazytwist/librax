package com.librax.lab.module.resource.controller.admin.resourceconfig.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 资源配置表,运行时锁状态见Redis分页 Request VO")
@Data
public class ResourceConfigPageReqVO extends PageParam {

    @Schema(description = "资源唯一ID,建议与设备ID一致,如 PH-METER-01、AGV-01", example = "19066")
    private String resourceId;

    @Schema(description = "资源类型,如 PH_METER / AGV / BENCH / TURBIDITY", example = "2")
    private String resourceType;

    @Schema(description = "归属类型:EXCLUSIVE 独占 / SHARED 共享", example = "2")
    private String ownershipType;

    @Schema(description = "独占时必填(归属区域),共享时为空")
    private String zoneCode;

    @Schema(description = "是否启用,0=禁用不参与调度")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}