-- ================================================================
-- AGV 正式设备 + 库位 + 搬运流程配置
-- AGV: http://172.16.50.4:8082
-- 流程: agv_transfer_flow_prod v1
-- ================================================================

-- 1. 正式 AGV 设备和标准接口指令
DELETE FROM lab_device_command WHERE device_type = 'AGV';
DELETE FROM lab_device_info WHERE device_id = 'AGV-01';

INSERT INTO lab_device_info (
    device_id, device_name, device_type, protocol, zone_code,
    host, port, base_url, callback_mode, poll_interval_ms,
    auth_type, auth_config, serial_port, baud_rate, data_bits, stop_bits, parity,
    sdk_class, sdk_config, connect_timeout_ms, read_timeout_ms,
    heartbeat_interval_ms, heartbeat_command, max_concurrent,
    status, enabled, remark,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'AGV-01', '正式AGV', 'AGV', 'HTTP', 'ZONE-A',
    '172.16.50.4', 8082, 'http://172.16.50.4:8082', 'WEBHOOK', NULL,
    'NONE', NULL, NULL, NULL, 8, 1, 'NONE',
    NULL, NULL, 5000, 120000,
    NULL, NULL, 1,
    'ONLINE', b'1', 'AGV标准接口正式设备',
    'admin', 'admin', NOW(3), NOW(3), b'0'
);

INSERT INTO lab_device_command (
    device_type, command_code, completion_mode, request_template, content_type,
    http_method, http_path, timeout_ms, retryable, codec_id,
    poll_path, poll_done_expr, poll_max_times, mock_output, remark,
    creator, updater, create_time, update_time, deleted
) VALUES
('AGV', 'startTask', 'SYNC',
 '{"taskId":"${taskId}","agvCmdList":"${agvCmdList}"}',
 'application/json', 'POST', '/device/agv/startTask', 120000, b'0', NULL,
 NULL, NULL, NULL, NULL, '标准接口：控制AGV执行step1/step2/step3组合任务',
 'admin', 'admin', NOW(3), NOW(3), b'0'),
('AGV', 'startAgain', 'SYNC',
 '{"taskId":"${taskId}"}',
 'application/json', 'POST', '/device/agv/startAgain', 30000, b'0', NULL,
 NULL, NULL, NULL, NULL, '标准接口：服务端通知AGV继续',
 'admin', 'admin', NOW(3), NOW(3), b'0');

-- 2. 统一 RACK/SLOT 正式基础数据
DELETE FROM lab_slot_info
WHERE rack_id IN ('AGV-01', 'ANALYZER-01', 'ENTRY-RACK', 'POST-PROCESS-RACK')
   OR slot_id LIKE 'R-A-%'
   OR slot_id LIKE 'R\\_C-1-%'
   OR slot_id LIKE 'R-E-%'
   OR slot_id LIKE 'R-PP-%';
DELETE FROM lab_rack_info WHERE rack_id IN ('AGV-01', 'ANALYZER-01', 'ENTRY-RACK', 'POST-PROCESS-RACK');

INSERT INTO lab_rack_info (
    rack_id, rack_name, rack_type, zone_code, coord_x, coord_y, coord_z,
    row_count, col_count, layer_count, enabled, remark,
    creator, updater, create_time, update_time, deleted
) VALUES
('AGV-01', 'AGV一号车载台', 'AGV', 'ZONE-A', NULL, NULL, NULL, 1, 8, 1, b'1', '移动载体，共8个位置；ZONE-A仅为管理归属区', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('ANALYZER-01', '分析仪一号载台', 'DEVICE', 'ZONE-A', NULL, NULL, NULL, 2, 4, 1, b'1', '分析仪内部8个位置', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('ENTRY-RACK', '货架出入口', 'RACK', 'ZONE-A', NULL, NULL, NULL, 1, 1, 3, b'1', '出入口3层', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('POST-PROCESS-RACK', '后处理货架', 'RACK', 'ZONE-A', NULL, NULL, NULL, 1, 4, 4, b'1', '后处理4层4列', 'admin', 'admin', NOW(3), NOW(3), b'0');

INSERT INTO lab_slot_info (
    slot_id, slot_name, slot_type, slot_usage, zone_code, rack_id,
    position_row, position_col, position_layer, coord_x, coord_y, coord_z,
    owner_id, local_index, capacity, status, current_count, occupied_by,
    enabled, remark, creator, updater, create_time, update_time, deleted
) VALUES
('R-A-1','AGV第一个位置','AGV','ANY',NULL,'AGV-01',1,1,1,NULL,NULL,NULL,NULL,'1',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-A-2','AGV第二个位置','AGV','ANY',NULL,'AGV-01',1,2,1,NULL,NULL,NULL,NULL,'2',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-A-3','AGV第三个位置','AGV','ANY',NULL,'AGV-01',1,3,1,NULL,NULL,NULL,NULL,'3',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-A-4','AGV第四个位置','AGV','ANY',NULL,'AGV-01',1,4,1,NULL,NULL,NULL,NULL,'4',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-A-5','AGV第五个位置','AGV','ANY',NULL,'AGV-01',1,5,1,NULL,NULL,NULL,NULL,'5',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-A-6','AGV第六个位置','AGV','ANY',NULL,'AGV-01',1,6,1,NULL,NULL,NULL,NULL,'6',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-A-7','AGV第七个位置','AGV','ANY',NULL,'AGV-01',1,7,1,NULL,NULL,NULL,NULL,'7',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-A-8','AGV第八个位置','AGV','ANY',NULL,'AGV-01',1,8,1,NULL,NULL,NULL,NULL,'8',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R_C-1-1-1','分析仪位置1-1','DEVICE','ANY','ZONE-A','ANALYZER-01',1,1,1,NULL,NULL,NULL,NULL,'1-1',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R_C-1-1-2','分析仪位置1-2','DEVICE','ANY','ZONE-A','ANALYZER-01',1,2,1,NULL,NULL,NULL,NULL,'1-2',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R_C-1-1-3','分析仪位置1-3','DEVICE','ANY','ZONE-A','ANALYZER-01',1,3,1,NULL,NULL,NULL,NULL,'1-3',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R_C-1-1-4','分析仪位置1-4','DEVICE','ANY','ZONE-A','ANALYZER-01',1,4,1,NULL,NULL,NULL,NULL,'1-4',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R_C-1-2-1','分析仪位置2-1','DEVICE','ANY','ZONE-A','ANALYZER-01',2,1,1,NULL,NULL,NULL,NULL,'2-1',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R_C-1-2-2','分析仪位置2-2','DEVICE','ANY','ZONE-A','ANALYZER-01',2,2,1,NULL,NULL,NULL,NULL,'2-2',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R_C-1-2-3','分析仪位置2-3','DEVICE','ANY','ZONE-A','ANALYZER-01',2,3,1,NULL,NULL,NULL,NULL,'2-3',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R_C-1-2-4','分析仪位置2-4','DEVICE','ANY','ZONE-A','ANALYZER-01',2,4,1,NULL,NULL,NULL,NULL,'2-4',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-E-1','货架出入口第一层','FIXED','ANY','ZONE-A','ENTRY-RACK',1,1,1,NULL,NULL,NULL,NULL,'1',1,'OCCUPIED',1,'TEST-CARRIER-001',b'1','默认测试源位','admin','admin',NOW(3),NOW(3),b'0'),
('R-E-2','货架出入口第二层','FIXED','ANY','ZONE-A','ENTRY-RACK',1,1,2,NULL,NULL,NULL,NULL,'2',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('R-E-3','货架出入口第三层','FIXED','ANY','ZONE-A','ENTRY-RACK',1,1,3,NULL,NULL,NULL,NULL,'3',1,'EMPTY',0,NULL,b'1',NULL,'admin','admin',NOW(3),NOW(3),b'0');

-- 后处理货架16位
INSERT INTO lab_slot_info (
    slot_id, slot_name, slot_type, slot_usage, zone_code, rack_id,
    position_row, position_col, position_layer, coord_x, coord_y, coord_z,
    owner_id, local_index, capacity, status, current_count, occupied_by,
    enabled, remark, creator, updater, create_time, update_time, deleted
)
SELECT CONCAT('R-PP-', l.n, '-', c.n), CONCAT('后处理货架第', l.n, '层第', c.n, '列'),
       'FIXED', 'ANY', 'ZONE-A', 'POST-PROCESS-RACK', 1, c.n, l.n,
       NULL, NULL, NULL, NULL, CONCAT(l.n, '-', c.n), 1, 'EMPTY', 0, NULL,
       b'1', NULL, 'admin', 'admin', NOW(3), NOW(3), b'0'
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) l
CROSS JOIN (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) c;

-- 3. 流程与步骤定义
DELETE FROM pd_pipeline_step WHERE pipeline_key = 'agv_transfer_flow_prod';
DELETE FROM pd_pipeline_definition WHERE pipeline_key = 'agv_transfer_flow_prod';
DELETE FROM pd_step_definition WHERE step_key IN (
 'agv_reserve_transfer_capacity','agv_build_standard_command','agv_start_task_prod',
 'agv_route_wait_signal','agv_wait_signal_prod','agv_optional_site_action',
 'agv_start_again_prod','agv_wait_complete_direct','agv_wait_complete_after_resume','agv_commit_transfer'
);

INSERT INTO pd_step_definition (
 step_key, version, name, description, step_type,
 device_type, command, executor, bean_name, method_name, chain_id,
 default_params, params_schema, output_fields,
 default_timeout_ms, default_max_attempts, default_backoff_ms,
 compensate_step_key, compensate_params, runnable_standalone, mock_output, status,
 creator, updater, create_time, update_time, deleted
) VALUES
('agv_reserve_transfer_capacity',1,'预留搬运库位','原子预留AGV中转位和目标位','COMPUTE',NULL,NULL,'BEAN','agvTransferStepExecutor','execute',NULL,'{}',NULL,NULL,30000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_build_standard_command',1,'组装AGV标准报文','生成taskId和agvCmdList','COMPUTE',NULL,NULL,'BEAN','agvTransferStepExecutor','execute',NULL,'{}',NULL,NULL,30000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_start_task_prod',1,'下发AGV任务','调用正式AGV startTask','INSTRUMENT','AGV','startTask',NULL,NULL,NULL,NULL,'{"disableSysFieldInjection":true}',NULL,NULL,120000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_route_wait_signal',1,'判断是否等待障碍信号','waitSignalEnabled条件路由','CONDITION',NULL,NULL,NULL,NULL,NULL,NULL,'{}',NULL,NULL,5000,1,1000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_wait_signal_prod',1,'等待AGV障碍信号','等待AGV调用waitSignal','WAIT',NULL,NULL,NULL,NULL,NULL,NULL,'{"waitReason":"等待AGV到达协同点"}',NULL,NULL,86400000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_optional_site_action',1,'可选现场动作','启用时等待人工完成开门等动作','COMPUTE',NULL,NULL,'BEAN','agvTransferStepExecutor','execute',NULL,'{}',NULL,NULL,86400000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_start_again_prod',1,'通知AGV继续','调用正式AGV startAgain','INSTRUMENT','AGV','startAgain',NULL,NULL,NULL,NULL,'{"disableSysFieldInjection":true}',NULL,NULL,30000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_wait_complete_direct',1,'等待AGV直达完成','无障碍路径等待taskState','WAIT',NULL,NULL,NULL,NULL,NULL,NULL,'{"waitReason":"等待AGV任务完成"}',NULL,NULL,1800000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_wait_complete_after_resume',1,'等待AGV恢复后完成','startAgain后等待taskState','WAIT',NULL,NULL,NULL,NULL,NULL,NULL,'{"waitReason":"等待AGV恢复后完成"}',NULL,NULL,1800000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0'),
('agv_commit_transfer',1,'提交搬运结果','提交源位、AGV位和目标位状态','COMPUTE',NULL,NULL,'BEAN','agvTransferStepExecutor','execute',NULL,'{}',NULL,NULL,30000,1,2000,NULL,NULL,b'0',NULL,'ACTIVE','admin','admin',NOW(3),NOW(3),b'0');

INSERT INTO pd_pipeline_definition (
 pipeline_key, version, name, description, fail_strategy, compensate_strategy,
 default_timeout_ms, default_max_attempts, default_backoff_ms, default_input_params,
 status, published_at, sample_mode, sample_bind_nodes,
 creator, updater, create_time, update_time, deleted
) VALUES (
 'agv_transfer_flow_prod',1,'AGV正式搬运流程','库位预留、标准报文、可选障碍等待、任务完成和位置提交','FAIL_FAST','NONE',
 3600000,1,2000,
 '{"waitSignalEnabled":false,"siteActionEnabled":false,"multiLoadEnabled":false,"sourceWindowCapacity":2,"agvCapacity":8,"allowPartialLoad":false,"loadStation":"AP1","siteActionPrompt":"请完成开门后确认","siteActionAssignee":"admin","taskType":"1","plateType":"Plate_5","transferItems":[{"instanceId":"TEST-CARRIER-001","sourceSlotId":"R-E-1","agvSlotId":"R-A-1","targetSlotId":"R_C-1-1-1","step1":{"taskName":"ENTRY_PICK","sourceArea":"R-E","sourcePos":"1","agvArea":"R-A","agvPos":"1","sourceStartPos":"1","count":"1","destinationStartPos":"1"},"step2":{"taskName":"AgvMove","agvStationName":"ANALYZER-01"},"step3":{"taskName":"ANALYZER_DROP","agvArea":"R-A","agvPos":"1","desArea":"R_C-1-1","desPos":"1","sourceStartPos":"1","count":"1","destinationStartPos":"1"}}]}',
 'ACTIVE',NOW(3),'NONE',NULL,'admin','admin',NOW(3),NOW(3),b'0'
);

-- 公共列顺序：定义身份/依赖/参数/策略/UI/资源/审计
INSERT INTO pd_pipeline_step (
 pipeline_key,pipeline_version,node_id,dispatch_mode,step_key,step_type,step_version,task_type,
 depends_on,condition_expr,true_branch,false_branch,branches,
 params_override,input_mapping,output_mapping,timeout_ms,max_attempts,backoff_ms,on_failure,
 compensate_node_id,compensate_step_key,compensate_params,compensate_on,command,runnable_standalone,mock_output,sort_order,ui_position,
 resource_enabled,zone_code,resource_wait_timeout_ms,creator,updater,create_time,update_time,deleted
) VALUES
('agv_transfer_flow_prod',1,'s_reserve_transfer','DIRECT','agv_reserve_transfer_capacity','COMPUTE',1,NULL,NULL,NULL,NULL,NULL,NULL,'{}',NULL,NULL,30000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,1,'{"x":80,"y":180}',b'0',NULL,NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_build_agv_command','DIRECT','agv_build_standard_command','COMPUTE',1,NULL,'["s_reserve_transfer"]',NULL,NULL,NULL,NULL,'{}','{"transferItems":"${s_reserve_transfer.transferItems}","taskId":"${s_reserve_transfer.taskId}"}',NULL,30000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,2,'{"x":280,"y":180}',b'0',NULL,NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_agv_start_task','DIRECT','agv_start_task_prod','INSTRUMENT',1,NULL,'["s_build_agv_command"]',NULL,NULL,NULL,NULL,'{}','{"taskId":"${s_build_agv_command.taskId}","agvCmdList":"${s_build_agv_command.agvCmdList}"}',NULL,120000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,3,'{"x":480,"y":180}',b'0','ZONE-A',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_route_wait_signal','DIRECT','agv_route_wait_signal','CONDITION',1,NULL,'["s_agv_start_task"]','waitSignalEnabled == true','s_agv_wait_signal','s_wait_agv_complete_direct','{"true":"s_agv_wait_signal","false":"s_wait_agv_complete_direct"}','{}',NULL,NULL,5000,1,1000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,4,'{"x":680,"y":180}',b'0',NULL,NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_agv_wait_signal','DIRECT','agv_wait_signal_prod','WAIT',1,NULL,'["s_route_wait_signal"]',NULL,NULL,NULL,NULL,'{}',NULL,NULL,86400000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,5,'{"x":880,"y":80}',b'0',NULL,NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_optional_site_action','DIRECT','agv_optional_site_action','COMPUTE',1,NULL,'["s_agv_wait_signal"]',NULL,NULL,NULL,NULL,'{}',NULL,NULL,86400000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,6,'{"x":1080,"y":80}',b'0',NULL,NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_agv_start_again','DIRECT','agv_start_again_prod','INSTRUMENT',1,NULL,'["s_optional_site_action"]',NULL,NULL,NULL,NULL,'{}','{"taskId":"${s_reserve_transfer.taskId}"}',NULL,30000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,7,'{"x":1280,"y":80}',b'0','ZONE-A',NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_wait_agv_complete_direct','DIRECT','agv_wait_complete_direct','WAIT',1,NULL,'["s_route_wait_signal"]',NULL,NULL,NULL,NULL,'{}',NULL,NULL,1800000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,8,'{"x":880,"y":300}',b'0',NULL,NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_wait_agv_complete_after_resume','DIRECT','agv_wait_complete_after_resume','WAIT',1,NULL,'["s_agv_start_again"]',NULL,NULL,NULL,NULL,'{}',NULL,NULL,1800000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,9,'{"x":1480,"y":80}',b'0',NULL,NULL,'admin','admin',NOW(3),NOW(3),b'0'),
('agv_transfer_flow_prod',1,'s_commit_transfer','DIRECT','agv_commit_transfer','COMPUTE',1,NULL,'["s_wait_agv_complete_direct","s_wait_agv_complete_after_resume"]',NULL,NULL,NULL,NULL,'{}','{"transferItems":"${s_reserve_transfer.transferItems}"}',NULL,30000,1,2000,'FAIL_PIPELINE',NULL,NULL,NULL,NULL,NULL,b'0',NULL,10,'{"x":1680,"y":180}',b'0',NULL,NULL,'admin','admin',NOW(3),NOW(3),b'0');

-- 执行后可通过 pipelineKey=agv_transfer_flow_prod, version=1 启动测试。
