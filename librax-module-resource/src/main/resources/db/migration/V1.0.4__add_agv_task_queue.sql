-- AGV 任务队列：串行化 startTask 下发，支持 AGV 忙时排队等候
CREATE TABLE IF NOT EXISTS lab_agv_task_queue (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id     VARCHAR(64)  NOT NULL COMMENT 'AGV 设备ID',
    plan_task_id  VARCHAR(128) NOT NULL COMMENT '对应 AgvLoadPlan.taskId（流程 executionId）',
    agv_task_id   VARCHAR(128) NOT NULL COMMENT '下发给 AGV 的任务ID',
    operation     VARCHAR(32)  NOT NULL COMMENT '操作类型：LOAD / MOVE / UNLOAD',
    payload       TEXT         NOT NULL COMMENT 'startTask 请求体 JSON',
    status        VARCHAR(32)  NOT NULL COMMENT 'PENDING / DISPATCHED / DONE / CANCELLED',
    create_time   DATETIME(3)  NOT NULL COMMENT '入队时间',
    dispatch_time DATETIME(3)  NULL     COMMENT '实际下发时间',
    PRIMARY KEY (id),
    KEY idx_agv_queue_device_status (device_id, status),
    KEY idx_agv_queue_agv_task_id   (agv_task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AGV 任务队列';

-- lab_agv_load_plan 新增 QUEUED 状态说明（已有 status 字段，无需 DDL，仅注释）
-- status 可选值：LOADING / WAIT_WAREHOUSE / WAIT_AGV_LOAD / WAIT_AGV_MOVE /
--               WAIT_AGV_UNLOAD / WAIT_SITE_ACTION / QUEUED /
--               LOAD_COMPLETE / WAIT_FLOW_COMMIT / COMPLETED / FAILED
