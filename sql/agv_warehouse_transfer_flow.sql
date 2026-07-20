-- 仓储单机械臂逐件备料 + AGV分阶段搬运流程

DELETE FROM lab_device_command WHERE device_type = 'WAREHOUSE' AND command_code = 'prepareMaterials';
DELETE FROM lab_device_info WHERE device_id = 'WAREHOUSE-01';

INSERT INTO lab_device_info (
    device_id, device_name, device_type, protocol, zone_code,
    host, port, base_url, callback_mode, auth_type,
    connect_timeout_ms, read_timeout_ms, max_concurrent,
    status, enabled, remark, creator, updater, create_time, update_time, deleted
) VALUES (
    'WAREHOUSE-01', '仓储机械臂', 'WAREHOUSE', 'HTTP', 'ZONE-A',
    'localhost', 10001, 'http://localhost:10001', 'WEBHOOK', 'NONE',
    5000, 30000, 1, 'ONLINE', b'1', '单机械臂，逐件备料',
    'admin', 'admin', NOW(3), NOW(3), b'0'
);

INSERT INTO lab_device_command (
    device_type, command_code, completion_mode, request_template, content_type,
    http_method, http_path, timeout_ms, retryable, codec_id,
    poll_path, poll_done_expr, poll_max_times, mock_output, remark,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'WAREHOUSE', 'prepareMaterials', 'SYNC',
    '{"requestId":"${requestId}","transferTaskId":"${transferTaskId}","materialId":"${materialId}","fromLocation":"${fromLocation}","toLocation":"${toLocation}","callbackUrl":"${callbackUrl}"}',
    'application/json', 'POST', '/prepareMaterials', 30000, b'0', NULL,
    NULL, NULL, NULL, NULL,
    '仓储逐件备料；HTTP响应只代表接单，最终结果通过slot/ready回调',
    'admin', 'admin', NOW(3), NOW(3), b'0'
);

DELETE FROM pd_pipeline_step WHERE pipeline_key = 'agv_warehouse_transfer_flow';
DELETE FROM pd_pipeline_definition WHERE pipeline_key = 'agv_warehouse_transfer_flow';
DELETE FROM pd_step_definition WHERE step_key = 'agv_start_warehouse_orchestration';

INSERT INTO pd_step_definition (
    step_key, version, name, description, step_type, executor, bean_name, method_name,
    default_params, default_timeout_ms, default_max_attempts, default_backoff_ms,
    runnable_standalone, status, creator, updater, create_time, update_time, deleted
) VALUES (
    'agv_start_warehouse_orchestration', 1, '仓储AGV统一编排',
    '仓储逐件备料，AGV分别执行装载、移动、卸载子任务',
    'COMPUTE', 'BEAN', 'agvTransferStepExecutor', 'execute', '{}',
    86400000, 1, 2000, b'0', 'ACTIVE',
    'admin', 'admin', NOW(3), NOW(3), b'0'
);

INSERT INTO pd_pipeline_definition (
    pipeline_key, version, name, description, fail_strategy, compensate_strategy,
    default_timeout_ms, default_max_attempts, default_backoff_ms, default_input_params,
    status, published_at, sample_mode, sample_bind_nodes,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'agv_warehouse_transfer_flow', 1, '仓储到设备AGV搬运流程',
    '服务端统一编排仓储单件备料和AGV step1/step2/step3独立子任务',
    'FAIL_FAST', 'NONE', 86400000, 1, 2000,
    '{"sourceWindowCapacity":2,"agvCapacity":8,"taskType":"1","plateType":"Plate_5","warehouseCallbackUrl":"http://localhost:48080/app-api/resource/slot/ready"}',
    'ACTIVE', NOW(3), 'NONE', NULL,
    'admin', 'admin', NOW(3), NOW(3), b'0'
);

INSERT INTO pd_pipeline_step (
    pipeline_key,pipeline_version,node_id,dispatch_mode,step_key,step_type,step_version,
    depends_on,params_override,input_mapping,timeout_ms,max_attempts,backoff_ms,on_failure,
    runnable_standalone,sort_order,ui_position,resource_enabled,
    creator,updater,create_time,update_time,deleted
) VALUES
('agv_warehouse_transfer_flow',1,'s_warehouse_orchestrate','DIRECT',
 'agv_start_warehouse_orchestration','COMPUTE',1,NULL,'{}',NULL,
 86400000,1,2000,'FAIL_PIPELINE',b'0',1,'{"x":120,"y":180}',b'0',
 'admin','admin',NOW(3),NOW(3),b'0'),
('agv_warehouse_transfer_flow',1,'s_commit_transfer','DIRECT',
 'agv_commit_transfer','COMPUTE',1,'["s_warehouse_orchestrate"]','{}',NULL,
 30000,1,2000,'FAIL_PIPELINE',b'0',2,'{"x":420,"y":180}',b'0',
 'admin','admin',NOW(3),NOW(3),b'0');

-- 仓储同步接单响应码建议：
-- 0=接单成功；1001=物料不足；1002=始发位置无效；1003=目标中转位不可用；
-- 1004=机械臂忙（可重试）；1005=requestId已处理（按幂等成功处理）；1099=仓储内部错误。
