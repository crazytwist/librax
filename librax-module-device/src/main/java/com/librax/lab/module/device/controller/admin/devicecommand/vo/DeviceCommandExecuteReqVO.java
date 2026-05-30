package com.librax.lab.module.device.controller.admin.devicecommand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 管理后台 - 设备指令直接执行 Request VO
 *
 * <p>不依赖流水线，直接触发设备执行指定指令。
 * 适用于设备联调、运维操作、人工干预等场景。
 */
@Schema(description = "管理后台 - 设备指令直接执行 Request VO")
@Data
public class DeviceCommandExecuteReqVO {

    @Schema(description = "指定执行的设备ID（可选，为空则自动从同类型设备中选择空闲设备）", example = "PH-METER-01")
    private String deviceId;

    @Schema(description = "设备类型，与 lab_device_info.device_type 对应", requiredMode = Schema.RequiredMode.REQUIRED, example = "PH_METER")
    @NotEmpty(message = "设备类型不能为空")
    private String deviceType;

    @Schema(description = "指令代码，如 MEASURE / CALIBRATE / RESET / START / PAUSE", requiredMode = Schema.RequiredMode.REQUIRED, example = "MEASURE")
    @NotEmpty(message = "指令代码不能为空")
    private String commandCode;

    @Schema(description = "执行参数，用于替换指令模板中的 ${xxx} 占位符")
    private Map<String, Object> params;

    @Schema(description = "执行超时时间(ms)，覆盖指令配置中的默认超时；为空则使用指令默认配置", example = "30000")
    @Min(value = 100, message = "超时时间最小 100ms")
    private Long timeoutMs;

}
