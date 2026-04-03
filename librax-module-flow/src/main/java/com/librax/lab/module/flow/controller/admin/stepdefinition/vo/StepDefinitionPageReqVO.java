package com.librax.lab.module.flow.controller.admin.stepdefinition.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 步骤定义表，可复用的步骤组件库 [pd_]分页 Request VO")
@Data
public class StepDefinitionPageReqVO extends PageParam {

    @Schema(description = "步骤唯一标识，小写_下划线，如 ph_measure")
    private String stepKey;

    @Schema(description = "步骤显示名称，如 PH检测", example = "李四")
    private String name;

    @Schema(description = "INSTRUMENT 仪器 | COMPUTE 计算 | CONDITION 条件 | WAIT 等待 | NOTIFY 通知", example = "2")
    private String stepType;

    @Schema(description = "设备类型，如 PH_METER，step_type=INSTRUMENT 时必填", example = "1")
    private String deviceType;

    @Schema(description = "默认设备指令，如 MEASURE，可被 pd_pipeline_step 覆盖")
    private String command;

    @Schema(description = "ACTIVE 启用  DISABLED 停用", example = "1")
    private String status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}