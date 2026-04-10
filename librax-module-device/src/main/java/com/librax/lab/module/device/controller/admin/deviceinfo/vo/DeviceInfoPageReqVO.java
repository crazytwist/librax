package com.librax.lab.module.device.controller.admin.deviceinfo.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 设备基本信息表，一行一台物理设备 [lab_device_]分页 Request VO")
@Data
public class DeviceInfoPageReqVO extends PageParam {

    @Schema(description = "设备唯一业务ID，如 PH-METER-01", example = "18227")
    private String deviceId;

    @Schema(description = "设备名称，如 雷磁PH计1号", example = "王五")
    private String deviceName;

    @Schema(description = "设备类型，对应 pd_step_definition.device_type，如 PH_METER", example = "1")
    private String deviceType;

    @Schema(description = "所属区域，关联 lab_zone_slot")
    private String zoneCode;

    @Schema(description = "当前状态：ONLINE/OFFLINE/FAULT，运行时由心跳维护，此处为初始值", example = "1")
    private String status;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}