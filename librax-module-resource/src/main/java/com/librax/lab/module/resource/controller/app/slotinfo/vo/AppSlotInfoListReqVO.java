package com.librax.lab.module.resource.controller.app.slotinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "用户 App - 库位列表 Request VO")
@Data
public class AppSlotInfoListReqVO {

    @Schema(description = "库位唯一编码", example = "RACK-L-R1-C1")
    private String slotId;

    @Schema(description = "库位显示名称", example = "载具柜-左侧 色谱瓶 R1C1")
    private String slotName;

    @Schema(description = "库位类型：FIXED / AGV / DEVICE", example = "FIXED")
    private String slotType;

    @Schema(description = "所属区域")
    private String zoneCode;

    @Schema(description = "所属货架编码", example = "RACK-L")
    private String rackId;

    @Schema(description = "占用状态：EMPTY / OCCUPIED / DISABLED")
    private String status;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
