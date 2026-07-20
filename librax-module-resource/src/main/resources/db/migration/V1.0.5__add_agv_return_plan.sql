-- 下料流程：AGV从站位取料，运回仓储中转位，仓储机械臂逐件入库
-- 前置：V1.0.4 已创建 lab_agv_task_queue

-- ① 为任务队列加 plan_type，区分补料(LOAD)与下料(RETURN)
ALTER TABLE lab_agv_task_queue
    ADD COLUMN IF NOT EXISTS plan_type VARCHAR(32) NOT NULL DEFAULT 'LOAD'
        COMMENT 'LOAD-补料计划 / RETURN-下料计划' AFTER operation;

-- ② 下料主计划
CREATE TABLE IF NOT EXISTS lab_agv_return_plan (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    task_id              VARCHAR(128)  NOT NULL COMMENT '流程执行ID（pipeline executionId）',
    device_id            VARCHAR(64)   NOT NULL COMMENT 'AGV设备ID',
    expected_count       INT           NOT NULL COMMENT '计划下料物料总数',
    returned_count       INT           NOT NULL DEFAULT 0 COMMENT '仓储已确认入库数量',
    wave_size            INT           NOT NULL DEFAULT 2 COMMENT '每波次中转位容量',
    current_wave         INT           NOT NULL DEFAULT 1 COMMENT '当前卸料波次',
    total_waves          INT           NOT NULL DEFAULT 1 COMMENT '总卸料波次',
    current_agv_task_id  VARCHAR(128)  NULL     COMMENT '当前AGV子任务ID',
    current_agv_operation VARCHAR(32)  NULL     COMMENT '当前AGV操作：LOAD/MOVE/UNLOAD',
    last_agv_station     VARCHAR(128)  NULL     COMMENT 'AGV最后上报站点',
    orchestration_node_id VARCHAR(96)  NULL     COMMENT '流程编排等待节点ID',
    warehouse_callback_url VARCHAR(256) NULL    COMMENT '仓储入库回调URL',
    task_type            VARCHAR(32)   NULL     COMMENT 'AGV任务类型',
    plate_type           VARCHAR(64)   NULL     COMMENT 'AGV托板类型',
    status               VARCHAR(32)   NOT NULL COMMENT 'WAIT_AGV_LOAD/WAIT_AGV_MOVE/WAIT_AGV_UNLOAD/WAIT_WAREHOUSE/QUEUED/WAIT_SITE_ACTION/WAIT_FLOW_COMMIT/COMPLETED/FAILED',
    creator              VARCHAR(64)   NOT NULL DEFAULT '',
    updater              VARCHAR(64)   NOT NULL DEFAULT '',
    create_time          DATETIME(3)   NOT NULL,
    update_time          DATETIME(3)   NOT NULL,
    deleted              BIT           NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_return_plan_task_id (task_id),
    KEY idx_return_plan_agv_task (current_agv_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AGV下料计划';

-- ③ 下料明细
CREATE TABLE IF NOT EXISTS lab_agv_return_item (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    task_id                   VARCHAR(128) NOT NULL COMMENT '流程执行ID',
    sequence_no               INT          NOT NULL COMMENT '序号（1-based）',
    wave_no                   INT          NOT NULL COMMENT '卸料波次（由transitCapacity决定）',
    source_slot_id            VARCHAR(96)  NOT NULL COMMENT '站位库位ID（AGV从此处取料）',
    agv_slot_id               VARCHAR(96)  NOT NULL COMMENT 'AGV载台槽位ID',
    transit_slot_id           VARCHAR(96)  NOT NULL COMMENT '仓储中转位ID（AGV卸料到此处）',
    warehouse_target_location VARCHAR(96)  NOT NULL COMMENT '仓储目标货架位置',
    instance_id               VARCHAR(96)  NOT NULL COMMENT '物料实例ID',
    container_type            VARCHAR(64)  NULL     COMMENT '容器类型',
    step1_json                TEXT         NULL     COMMENT 'LOAD机械臂指令：站位→AGV槽',
    step2_json                TEXT         NULL     COMMENT 'MOVE移动指令（仅第一件存储）',
    step3_json                TEXT         NULL     COMMENT 'UNLOAD机械臂指令：AGV槽→中转位',
    warehouse_request_id      VARCHAR(128) NULL     COMMENT 'returnMaterials子请求ID',
    status                    VARCHAR(32)  NOT NULL COMMENT 'WAIT_LOAD/ON_AGV/IN_TRANSIT/RETURNED/COMPLETED/FAILED',
    dispatched_at             DATETIME(3)  NULL,
    loaded_at                 DATETIME(3)  NULL,
    transit_at                DATETIME(3)  NULL,
    returned_at               DATETIME(3)  NULL,
    creator                   VARCHAR(64)  NOT NULL DEFAULT '',
    updater                   VARCHAR(64)  NOT NULL DEFAULT '',
    create_time               DATETIME(3)  NOT NULL,
    update_time               DATETIME(3)  NOT NULL,
    deleted                   BIT          NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_return_item (task_id, sequence_no),
    KEY idx_return_item_wave (task_id, wave_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AGV下料明细';

-- ④ 仓储下料命令（入库：从中转位放回货架）
DELETE FROM lab_device_command WHERE device_type = 'WAREHOUSE' AND command_code = 'returnMaterials';
INSERT INTO lab_device_command (
    device_type, command_code, completion_mode, request_template, content_type,
    http_method, http_path, timeout_ms, retryable, codec_id,
    poll_path, poll_done_expr, poll_max_times, mock_output, remark,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'WAREHOUSE', 'returnMaterials', 'SYNC',
    '{"executionId":"${executionId}","containerType":"${containerType}","fromLocation":"${fromLocation}","toLocation":"${toLocation}","callbackUrl":"${callbackUrl}"}',
    'application/json', 'POST', '/returnMaterials', 30000, b'0', NULL, NULL, NULL, NULL, NULL,
    '0接单；1001物料不存在；1002中转位无物料；1003目标位不可用；1004机械臂忙；1005幂等成功；1099内部错误',
    'admin', 'admin', NOW(3), NOW(3), b'0'
);

-- ⑤ 流程步骤与流水线定义
DELETE FROM pd_pipeline_step WHERE pipeline_key = 'agv_warehouse_return_flow';
DELETE FROM pd_pipeline_definition WHERE pipeline_key = 'agv_warehouse_return_flow';
DELETE FROM pd_step_definition WHERE step_key IN ('agv_start_return_orchestration', 'agv_commit_return');

INSERT INTO pd_step_definition (
    step_key, version, name, description, step_type, executor, bean_name, method_name,
    default_params, default_timeout_ms, default_max_attempts, default_backoff_ms,
    runnable_standalone, status, creator, updater, create_time, update_time, deleted
) VALUES
('agv_start_return_orchestration', 1, 'AGV return orchestration',
 'AGV picks all materials from lab; unloads to warehouse transit wave by wave; warehouse stores each item',
 'COMPUTE', 'BEAN', 'agvStartReturnOrchestrationBean', 'execute', '{}',
 86400000, 1, 2000, b'0', 'ACTIVE', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('agv_commit_return', 1, 'AGV return commit',
 'Finalize slot occupancy after all materials returned to warehouse',
 'COMPUTE', 'BEAN', 'agvCommitReturnBean', 'execute', '{}',
 30000, 1, 2000, b'0', 'ACTIVE', 'admin', 'admin', NOW(3), NOW(3), b'0');

INSERT INTO pd_pipeline_definition (
    pipeline_key, version, name, description, fail_strategy, compensate_strategy,
    default_timeout_ms, default_max_attempts, default_backoff_ms, default_input_params,
    status, published_at, sample_mode, sample_bind_nodes,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'agv_warehouse_return_flow', 1, '下料单车运输子流程',
    'AGV装载全部物料后，按中转位容量分波次卸料，仓储机械臂逐件入库后再卸下一波',
    'FAIL_FAST', 'NONE', 86400000, 1, 2000,
    '{"transitCapacity":2,"agvCapacity":8,"taskType":"1","plateType":"2","warehouseCallbackUrl":"http://localhost:48080/app-api/resource/slot/returned"}',
    'ACTIVE', NOW(3), 'NONE', NULL,
    'admin', 'admin', NOW(3), NOW(3), b'0'
);

INSERT INTO pd_pipeline_step (
    pipeline_key, pipeline_version, node_id, dispatch_mode, step_key, step_type, step_version,
    depends_on, params_override, input_mapping, timeout_ms, max_attempts, backoff_ms, on_failure,
    runnable_standalone, sort_order, ui_position, resource_enabled,
    creator, updater, create_time, update_time, deleted
) VALUES
('agv_warehouse_return_flow', 1, 's_return_orchestrate', 'DIRECT',
 'agv_start_return_orchestration', 'COMPUTE', 1, NULL, '{}', NULL,
 86400000, 1, 2000, 'FAIL_FAST', b'0', 1, '{"x":120,"y":180}', b'0',
 'admin', 'admin', NOW(3), NOW(3), b'0'),
('agv_warehouse_return_flow', 1, 's_commit_return', 'DIRECT',
 'agv_commit_return', 'COMPUTE', 1, '["s_return_orchestrate"]', '{}', NULL,
 30000, 1, 2000, 'FAIL_FAST', b'0', 2, '{"x":420,"y":180}', b'0',
 'admin', 'admin', NOW(3), NOW(3), b'0');
