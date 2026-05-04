package com.librax.lab.module.resource.controller.admin.slotinfo.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 库位定义，slot_id 同时作为资源ID纳入调度，归 resource 模块管理分页 Request VO")
@Data
public class SlotInfoPageReqVO extends PageParam {

    @Schema(description = "库位唯一编码，同时作为 lab_resource_config.resource_id。固定库位如 RACK-A-01，AGV库位如 AGV-01-SLOT-1", example = "9968")
    private String slotId;

    @Schema(description = "库位显示名称，如 A区货架第1槽", example = "赵六")
    private String slotName;

    @Schema(description = "库位类型：FIXED=固定台面库位（有世界坐标） AGV=AGV载台槽位（无固定坐标） DEVICE=设备内部位置", example = "2")
    private String slotType;

    @Schema(description = "所属区域，FIXED和DEVICE类型必填，AGV类型为NULL（跟随AGV移动）")
    private String zoneCode;

    @Schema(description = "所属货架编码，关联 lab_rack_info.rack_id，仅 FIXED 类型填写", example = "11492")
    private String rackId;

    @Schema(description = "是否启用：1=启用参与调度 0=禁用（维修/封存时禁用）")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}