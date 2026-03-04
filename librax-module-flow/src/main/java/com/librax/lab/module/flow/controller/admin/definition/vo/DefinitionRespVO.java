package com.librax.lab.module.flow.controller.admin.definition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 流程定义 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DefinitionRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "21207")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "流程ID", example = "27463")
    @ExcelProperty("流程ID")
    private String flowId;

    @Schema(description = "流程名称", example = "赵六")
    @ExcelProperty("流程名称")
    private String flowName;

    @Schema(description = "流程描述")
    @ExcelProperty("流程描述")
    private String flowDesc;

    @Schema(description = "流程启动ID", example = "18873")
    @ExcelProperty("流程启动ID")
    private String startNodeId;

    @Schema(description = "流程状态", example = "2")
    @ExcelProperty("流程状态")
    private String flowStatus;

    @Schema(description = "流程包含的节点ID列表（JSON数组）")
    @ExcelProperty("流程包含的节点ID列表（JSON数组）")
    private String nodeIds;

    @Schema(description = "流程流转规则（JSON数组，含源节点、目标节点、条件等）")
    @ExcelProperty("流程流转规则（JSON数组，含源节点、目标节点、条件等）")
    private String flowRules;

    @Schema(description = "额外字段1")
    @ExcelProperty("额外字段1")
    private String ext1;

    @Schema(description = "额外字段2")
    @ExcelProperty("额外字段2")
    private String ext2;

    @Schema(description = "额外Json1")
    @ExcelProperty("额外Json1")
    private String extJson1;

    @Schema(description = "额外Json2")
    @ExcelProperty("额外Json2")
    private String extJson2;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}