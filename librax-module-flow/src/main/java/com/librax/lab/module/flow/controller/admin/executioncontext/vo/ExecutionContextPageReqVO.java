package com.librax.lab.module.flow.controller.admin.executioncontext.vo;

import lombok.*;

import java.util.*;

import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用分页 Request VO")
@Data
public class ExecutionContextPageReqVO extends PageParam {

    @Schema(description = "关联 pe_pipeline_execution.execution_id", example = "5932")
    private String executionId;

    @Schema(description = "已完成节点的输出汇总，key 为 node_id")
    private String contextData;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}