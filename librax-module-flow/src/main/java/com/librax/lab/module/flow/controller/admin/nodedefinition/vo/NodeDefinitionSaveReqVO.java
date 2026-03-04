package com.librax.lab.module.flow.controller.admin.nodedefinition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 流程节点定义新增/修改 Request VO")
@Data
public class NodeDefinitionSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "976")
    private Long id;

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

}