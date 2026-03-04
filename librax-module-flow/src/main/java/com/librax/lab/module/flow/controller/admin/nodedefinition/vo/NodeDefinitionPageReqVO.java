package com.librax.lab.module.flow.controller.admin.nodedefinition.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 流程节点定义分页 Request VO")
@Data
public class NodeDefinitionPageReqVO extends PageParam {

    @Schema(description = "节点ID", example = "11385")
    private String nodeId;

    @Schema(description = "节点名称", example = "李四")
    private String nodeName;

    @Schema(description = "节点编码")
    private String nodeCode;

    @Schema(description = "节点类型", example = "2")
    private String nodeType;

    @Schema(description = "节点类别")
    private String nodeCategory;

    @Schema(description = "节点描述")
    private String nodeDesc;

    @Schema(description = "节点状态", example = "2")
    private String nodeStatus;

    @Schema(description = "执行配置")
    private String executeConfig;

    @Schema(description = "节点参数")
    private String nodeParams;

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