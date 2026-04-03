package com.librax.lab.module.flow.dal.dataobject.executioncontext;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用 DO
 *
 * @author 一南
 */
@TableName("pe_execution_context")
@KeySequence("pe_execution_context_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContextDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 关联 pe_pipeline_execution.execution_id
     */
    private String executionId;
    /**
     * 已完成节点的输出汇总，key 为 node_id，如 {"s_ph":{"ph":7.2}}
     */
    private String contextData;


}