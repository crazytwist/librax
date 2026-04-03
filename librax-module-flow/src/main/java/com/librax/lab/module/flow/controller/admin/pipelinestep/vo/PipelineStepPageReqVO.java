package com.librax.lab.module.flow.controller.admin.pipelinestep.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 流程步骤关联表，定义节点编排关系、参数配置与补偿策略 [pd_]分页 Request VO")
@Data
public class PipelineStepPageReqVO extends PageParam {

    @Schema(description = "关联 pd_pipeline_definition.pipeline_key")
    private String pipelineKey;

    @Schema(description = "节点 ID，同一流程版本内唯一，小写+下划线，如 s_ph", example = "5566")
    private String nodeId;

    @Schema(description = "关联 pd_step_definition.step_key")
    private String stepKey;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}