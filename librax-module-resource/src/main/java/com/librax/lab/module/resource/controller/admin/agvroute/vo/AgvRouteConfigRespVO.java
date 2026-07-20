package com.librax.lab.module.resource.controller.admin.agvroute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AGV 路由配置 - 响应 VO。
 */
@Data
@Schema(description = "AGV路由配置 - 响应")
public class AgvRouteConfigRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "路由唯一编码，如 HCL_TO_WAREHOUSE")
    private String routeCode;

    @Schema(description = "路由名称")
    private String routeName;

    @Schema(description = "起始站点编码，如 HCL / WAREHOUSE / STX / FX")
    private String sourceStationCode;

    @Schema(description = "目标站点编码，如 WAREHOUSE / HCL / STX / FX")
    private String destStationCode;

    @Schema(description = "AGV任务类型（透传AGV设备）")
    private String taskType;

    @Schema(description = "AGV托板类型（透传AGV设备）")
    private String plateType;

    @Schema(description = "step1工作流编码：物料装载到AGV（如 HCL_AGV / CC_AGV）")
    private String step1TaskName;

    @Schema(description = "step2工作流编码：AGV整体移动（通常为 AgvMove）")
    private String step2TaskName;

    @Schema(description = "step2目标站点名：AGV移动目的地（须与AGV地图站点名称一致）")
    private String step2StationName;

    @Schema(description = "step3工作流编码：AGV卸载到目标位（如 AGV_CC / AGV_HCL）")
    private String step3TaskName;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后更新者")
    private String updater;

    @Schema(description = "最后更新时间")
    private LocalDateTime updateTime;
}
