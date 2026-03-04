package com.librax.lab.module.flow.controller.admin.definition.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 流程定义分页 Request VO")
@Data
public class DefinitionPageReqVO extends PageParam {

    @Schema(description = "流程ID", example = "27463")
    private String flowId;

    @Schema(description = "流程名称", example = "赵六")
    private String flowName;

    @Schema(description = "流程描述")
    private String flowDesc;

    @Schema(description = "流程启动ID", example = "18873")
    private String startNodeId;

    @Schema(description = "流程状态", example = "2")
    private String flowStatus;

    @Schema(description = "流程包含的节点ID列表（JSON数组）")
    private String nodeIds;

    @Schema(description = "流程流转规则（JSON数组，含源节点、目标节点、条件等）")
    private String flowRules;

    @Schema(description = "额外字段1")
    private String ext1;

    @Schema(description = "额外字段2")
    private String ext2;

    @Schema(description = "额外Json1")
    private String extJson1;

    @Schema(description = "额外Json2")
    private String extJson2;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}