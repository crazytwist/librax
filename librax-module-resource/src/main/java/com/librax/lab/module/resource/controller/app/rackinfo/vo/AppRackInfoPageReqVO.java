package com.librax.lab.module.resource.controller.app.rackinfo.vo;

import com.librax.lab.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "用户 App - 货架分页 Request VO")
@Data
public class AppRackInfoPageReqVO extends PageParam {

    @Schema(description = "货架唯一编码", example = "RACK-L")
    private String rackId;

    @Schema(description = "货架名称", example = "载具柜-左侧")
    private String rackName;

    @Schema(description = "货架类型：RACK / BENCH / INCUBATOR / FREEZER", example = "RACK")
    private String rackType;

    @Schema(description = "所属区域")
    private String zoneCode;

    @Schema(description = "货架行数")
    private Integer rowCount;

    @Schema(description = "货架列数")
    private Integer colCount;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
