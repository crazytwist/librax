package com.librax.lab.module.lab.controller.admin.samplestep.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态分页 Request VO")
@Data
public class SampleStepPageReqVO extends PageParam {

    @Schema(description = "样本ID", example = "31248")
    private String sampleId;

    @Schema(description = "流程执行ID", example = "29482")
    private String executionId;

    @Schema(description = "步骤节点ID", example = "28608")
    private String nodeId;

    @Schema(description = "BOUND已绑定 / PROCESSING处理中 / COMPLETED已完成 / FAILED失败 / UNBOUND已解绑", example = "2")
    private String status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}