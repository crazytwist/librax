package com.librax.lab.module.task.stub;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgvJob {

    /** 起点站点编码 */
    private String from;

    /** 终点站点编码 */
    private String to;

    /** 物料/样本ID */
    private String materialId;

    /** 优先级，透传给调度端 */
    private Integer priority;

    /**
     * 回调令牌
     * 调度端任务完成后，必须携带此 token 回调
     * POST /app-api/task/agv/callback
     */
    private String callbackToken;
}