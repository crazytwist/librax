package com.librax.lab.module.flow.dal.dataobject.nodedefinition;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 流程节点定义 DO
 *
 * @author 芋道源码
 */
@TableName("flow_node_definition")
@KeySequence("flow_node_definition_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeDefinitionDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 节点ID
     */
    private String nodeId;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 节点编码
     */
    private String nodeCode;
    /**
     * 节点类型
     */
    private String nodeType;
    /**
     * 节点类别
     */
    private String nodeCategory;
    /**
     * 节点描述
     */
    private String nodeDesc;
    /**
     * 节点状态
     */
    private String nodeStatus;
    /**
     * 执行配置
     */
    private String executeConfig;
    /**
     * 节点参数
     */
    private String nodeParams;
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