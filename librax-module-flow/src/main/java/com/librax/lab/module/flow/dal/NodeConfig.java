package com.librax.lab.module.flow.dal;

import com.librax.lab.module.flow.enums.NodeTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeConfig {

    /** 数据库节点ID */
    private String nodeId;

    /** 节点类型 */
    private NodeTypeEnum type;

    /** 节点业务参数 */
    private Map<String, Object> params;

    /** 是否是终止节点 */
    private boolean end;
}
