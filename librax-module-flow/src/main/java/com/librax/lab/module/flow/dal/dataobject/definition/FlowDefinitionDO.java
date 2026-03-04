package com.librax.lab.module.flow.dal.dataobject.definition;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 流程定义 DO
 *
 * @author 芋道源码
 */
@TableName("flow_definition")
@KeySequence("flow_definition_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowDefinitionDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 流程ID
     */
    private String flowId;
    /**
     * 流程名称
     */
    private String flowName;
    /**
     * 流程描述
     */
    private String flowDesc;
    /**
     * 流程启动ID
     */
    private String startNodeId;
    /**
     * 流程状态
     */
    private String flowStatus;
    /**
     * 流程包含的节点ID列表（JSON数组）
     */
    private String nodeIds;
    /**
     * 流程流转规则（JSON数组，含源节点、目标节点、条件等）
     */
    private String flowRules;
    /**
     * 额外字段1
     */
    private String ext1;
    /**
     * 额外字段2
     */
    private String ext2;
    /**
     * 额外Json1
     */
    private String extJson1;
    /**
     * 额外Json2
     */
    private String extJson2;


}