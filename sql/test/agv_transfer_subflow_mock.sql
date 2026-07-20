-- AGV 搬运子流程 mock 配置
--
-- 设计说明：
-- 1. /device/agv/startTask 是下发给 AGV 的组合任务接口，报文严格为 taskId + command。
-- 2. /device/agv/waitSignal 是 AGV 回调服务端的“到达等待点”事件，不建议建成普通条件节点。
--    当前配置用 WAIT 节点 agv_wait_signal_gate 表示“等待 AGV 到达等待点”；
--    waitSignal 适配器收到事件后回调完成这个 WAIT 节点。
-- 3. WAIT 节点完成后，流程先执行现场动作（示例为 DOOR_MOCK.openDoor），再调用 startAgain。
-- 4. /device/agv/startAgain 是服务端通知 AGV 继续执行的下发接口，建成独立 INSTRUMENT 节点。
-- 5. 本地为了避免打到已有 HTTP AGV-01/02，使用 AGV_MOCK 设备类型；正式接入时可复制为 AGV。

-- ------------------------------------------------------------------
-- 1. AGV mock 指令
-- ------------------------------------------------------------------

DELETE FROM lab_device_command WHERE device_type = 'AGV_MOCK';
DELETE FROM lab_device_command WHERE device_type = 'DOOR_MOCK';

INSERT INTO lab_device_command (
    device_type, command_code, completion_mode, request_template, content_type,
    http_method, http_path, timeout_ms, retryable, codec_id,
    poll_path, poll_done_expr, poll_max_times, mock_output, remark,
    creator, updater, create_time, update_time, deleted
) VALUES
(
    'AGV_MOCK',
    'startTask',
    'SYNC',
    '{"taskId":"${taskId}","command":"${command}"}',
    'application/json',
    'POST',
    '/device/agv/startTask',
    120000,
    b'1',
    NULL,
    NULL,
    NULL,
    60,
    '{"code":0,"message":"mock startTask accepted"}',
    'AGV 标准接口：控制 AGV 执行任务。请求体严格为 taskId + command；mock 模式设为 SYNC。',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'AGV_MOCK',
    'startAgain',
    'SYNC',
    '{"taskId":"${taskId}"}',
    'application/json',
    'POST',
    '/device/agv/startAgain',
    30000,
    b'1',
    NULL,
    NULL,
    NULL,
    10,
    '{"code":0,"message":"mock startAgain accepted"}',
    'AGV 标准接口：服务端通知 AGV 继续执行。请求体严格为 taskId。',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'DOOR_MOCK',
    'openDoor',
    'SYNC',
    '{"action":"openDoor","reason":"agvWaitSignal","executionId":"${executionId}","nodeId":"${nodeId}"}',
    'application/json',
    'POST',
    '/device/door/open',
    30000,
    b'1',
    NULL,
    NULL,
    NULL,
    10,
    '{"code":0,"message":"mock door opened"}',
    'AGV 等待点现场动作示例：开门/开闸/解锁。真实接入时替换为门禁或自动门设备类型。',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
);

-- ------------------------------------------------------------------
-- 2. AGV mock 设备与资源
-- ------------------------------------------------------------------

DELETE FROM lab_device_info WHERE device_id = 'AGV-MOCK-01';
DELETE FROM lab_device_info WHERE device_id = 'DOOR-MOCK-01';
INSERT INTO lab_device_info (
    device_id, device_name, device_type, protocol, zone_code,
    host, port, base_url, callback_mode, poll_interval_ms,
    auth_type, auth_config, serial_port, baud_rate, data_bits, stop_bits, parity,
    sdk_class, sdk_config, connect_timeout_ms, read_timeout_ms,
    heartbeat_interval_ms, heartbeat_command, max_concurrent,
    status, enabled, remark,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'AGV-MOCK-01',
    'AGV Mock 调试车',
    'AGV_MOCK',
    'MOCK',
    'ZONE-A',
    '127.0.0.1',
    0,
    NULL,
    'WEBHOOK',
    NULL,
    'NONE',
    NULL,
    NULL,
    NULL,
    8,
    1,
    'NONE',
    NULL,
    NULL,
    5000,
    30000,
    NULL,
    NULL,
    1,
    'ONLINE',
    b'1',
    '本地 AGV 流程验证专用，不访问真实 AGV 服务。',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'DOOR-MOCK-01',
    '门禁 Mock 设备',
    'DOOR_MOCK',
    'MOCK',
    'ZONE-A',
    '127.0.0.1',
    0,
    NULL,
    'WEBHOOK',
    NULL,
    'NONE',
    NULL,
    NULL,
    NULL,
    8,
    1,
    'NONE',
    NULL,
    NULL,
    5000,
    30000,
    NULL,
    NULL,
    1,
    'ONLINE',
    b'1',
    'AGV 等待点开门动作 mock 设备，不访问真实门禁服务。',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
);

DELETE FROM lab_resource_config WHERE resource_id = 'AGV-MOCK-01';
DELETE FROM lab_resource_config WHERE resource_id = 'DOOR-MOCK-01';
INSERT INTO lab_resource_config (
    resource_id, resource_type, ownership_type, zone_code, max_concurrent,
    enabled, remark, creator, updater, create_time, update_time, deleted
) VALUES (
    'AGV-MOCK-01',
    'AGV_MOCK',
    'EXCLUSIVE',
    'ZONE-A',
    1,
    b'1',
    'AGV mock 独占资源，供 agv_transfer_subflow_mock 使用。',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'DOOR-MOCK-01',
    'DOOR_MOCK',
    'EXCLUSIVE',
    'ZONE-A',
    1,
    b'1',
    'AGV 等待点现场动作 mock 资源，供 door_open_for_agv 使用。',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
);

-- ------------------------------------------------------------------
-- 3. 步骤定义
-- ------------------------------------------------------------------

DELETE FROM pd_pipeline_step WHERE pipeline_key = 'agv_transfer_subflow_mock';
DELETE FROM pd_pipeline_definition WHERE pipeline_key = 'agv_transfer_subflow_mock';
DELETE FROM pd_step_definition
WHERE step_key IN ('agv_start_task_batch', 'agv_wait_signal_gate', 'agv_start_again');
DELETE FROM pd_step_definition
WHERE step_key IN ('door_open_for_agv');

INSERT INTO pd_step_definition (
    step_key, version, name, description, step_type,
    device_type, command, executor, bean_name, method_name, chain_id,
    default_params, params_schema, output_fields,
    default_timeout_ms, default_max_attempts, default_backoff_ms,
    compensate_step_key, compensate_params,
    runnable_standalone, mock_output, status,
    creator, updater, create_time, update_time, deleted
) VALUES
(
    'agv_start_task_batch',
    1,
    'AGV执行组合任务',
    '按标准接口 /device/agv/startTask 下发 step1/step2/step3 组合任务；发送给 AGV 的报文严格为 taskId + command。',
    'INSTRUMENT',
    'AGV_MOCK',
    'startTask',
    NULL,
    NULL,
    NULL,
    NULL,
    '{
      "disableSysFieldInjection": true,
      "command": [
        {
          "step": "step1",
          "taskName": "取载具到AGV",
          "sourceArea": "A",
          "sourcePos": "1",
          "agvArea": "B",
          "agvPos": "2",
          "sourceStartPos": "1",
          "count": "1",
          "destinationStartPos": "1"
        },
        {
          "step": "step2",
          "taskName": "AGV移动",
          "agvStationName": "LM10"
        },
        {
          "step": "step3",
          "taskName": "从AGV放到目标位置",
          "agvArea": "B",
          "agvPos": "2",
          "desArea": "C",
          "desPos": "1",
          "sourceStartPos": "1",
          "count": "10",
          "destinationStartPos": "1"
        }
      ]
    }',
    '{
      "type": "object",
      "required": ["taskId", "command"],
      "properties": {
        "taskId": {
          "type": "string",
          "description": "AGV 任务号，默认使用 executionId_nodeId"
        },
        "command": {
          "type": "array",
          "description": "AGV 动作列表；step1/step2/step3 可组合，也可只传其中一类"
        }
      }
    }',
    '["deviceTaskId","deviceType","command"]',
    120000,
    1,
    2000,
    NULL,
    NULL,
    b'1',
    '{"deviceTaskId":"AGV-MOCK-TASK","deviceType":"AGV_MOCK","command":"startTask"}',
    'ACTIVE',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'agv_wait_signal_gate',
    1,
    '等待AGV到达等待点',
    '表达服务端等待 AGV 调用 /device/agv/waitSignal；收到 waitSignal 后完成该 WAIT 节点，并进入开门等现场动作。',
    'WAIT',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '{"waitReason":"AGV 已到达等待点，等待服务端确认后调用 startAgain","assignee":"admin"}',
    '{"type":"object","properties":{"waitReason":{"type":"string"},"assignee":{"type":"string"}}}',
    '["waitReason","assignee"]',
    86400000,
    1,
    2000,
    NULL,
    NULL,
    b'1',
    NULL,
    'ACTIVE',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'door_open_for_agv',
    1,
    'AGV等待点开门',
    'AGV 到达等待点后，服务端执行开门/开闸/让位等现场动作；动作完成后才能调用 startAgain。',
    'INSTRUMENT',
    'DOOR_MOCK',
    'openDoor',
    NULL,
    NULL,
    NULL,
    NULL,
    '{"disableSysFieldInjection":true}',
    '{"type":"object","properties":{"action":{"type":"string","description":"现场动作，示例 openDoor"}}}',
    '["deviceTaskId","deviceType","command"]',
    30000,
    1,
    2000,
    NULL,
    NULL,
    b'1',
    '{"deviceTaskId":"DOOR-MOCK-OPEN","deviceType":"DOOR_MOCK","command":"openDoor"}',
    'ACTIVE',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'agv_start_again',
    1,
    '通知AGV继续',
    '按标准接口 /device/agv/startAgain 通知 AGV 从等待点继续执行。',
    'INSTRUMENT',
    'AGV_MOCK',
    'startAgain',
    NULL,
    NULL,
    NULL,
    NULL,
    '{"disableSysFieldInjection":true}',
    '{"type":"object","required":["taskId"],"properties":{"taskId":{"type":"string","description":"默认使用 executionId_s_agv_start_task，与 startTask 下发任务号一致"}}}',
    '["deviceTaskId","deviceType","command"]',
    30000,
    1,
    2000,
    NULL,
    NULL,
    b'1',
    '{"deviceTaskId":"AGV-MOCK-START-AGAIN","deviceType":"AGV_MOCK","command":"startAgain"}',
    'ACTIVE',
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
);

-- ------------------------------------------------------------------
-- 4. 流程定义
-- ------------------------------------------------------------------

INSERT INTO pd_pipeline_definition (
    pipeline_key, version, name, description,
    fail_strategy, compensate_strategy,
    default_timeout_ms, default_max_attempts, default_backoff_ms,
    default_input_params, status, published_at,
    sample_mode, sample_bind_nodes,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'agv_transfer_subflow_mock',
    1,
    'AGV搬运子流程Mock',
    'AGV 标准接口子流程：startTask 下发组合动作，waitSignal 表示 AGV 到达等待点，随后开门，最后 startAgain 通知 AGV 继续。',
    'FAIL_FAST',
    'NONE',
    600000,
    1,
    2000,
    '{
      "taskId": "TASK_20260516_001",
      "command": [
        {
          "step": "step1",
          "taskName": "取载具到AGV",
          "sourceArea": "A",
          "sourcePos": "1",
          "agvArea": "B",
          "agvPos": "2",
          "sourceStartPos": "1",
          "count": "1",
          "destinationStartPos": "1"
        },
        {
          "step": "step2",
          "taskName": "AGV移动",
          "agvStationName": "LM10"
        },
        {
          "step": "step3",
          "taskName": "从AGV放到目标位置",
          "agvArea": "B",
          "agvPos": "2",
          "desArea": "C",
          "desPos": "1",
          "sourceStartPos": "1",
          "count": "10",
          "destinationStartPos": "1"
        }
      ]
    }',
    'ACTIVE',
    NOW(3),
    'NONE',
    NULL,
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
);

INSERT INTO pd_pipeline_step (
    pipeline_key, pipeline_version, node_id,
    dispatch_mode, step_key, step_type, step_version, task_type,
    depends_on, condition_expr, true_branch, false_branch, branches,
    params_override, input_mapping, output_mapping,
    timeout_ms, max_attempts, backoff_ms, on_failure,
    compensate_node_id, compensate_step_key, compensate_params, compensate_on,
    command, runnable_standalone, mock_output, sort_order, ui_position,
    resource_enabled, zone_code, resource_wait_timeout_ms,
    creator, updater, create_time, update_time, deleted
) VALUES
(
    'agv_transfer_subflow_mock',
    1,
    's_agv_start_task',
    'DIRECT',
    'agv_start_task_batch',
    'INSTRUMENT',
    1,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '{}',
    NULL,
    NULL,
    120000,
    1,
    2000,
    'FAIL_PIPELINE',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    b'1',
    NULL,
    1,
    '{"x":120,"y":160}',
    b'1',
    'ZONE-A',
    300000,
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'agv_transfer_subflow_mock',
    1,
    's_agv_wait_signal',
    'DIRECT',
    'agv_wait_signal_gate',
    'WAIT',
    1,
    NULL,
    '["s_agv_start_task"]',
    NULL,
    NULL,
    NULL,
    NULL,
    '{}',
    NULL,
    NULL,
    86400000,
    1,
    2000,
    'FAIL_PIPELINE',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    b'1',
    NULL,
    2,
    '{"x":420,"y":160}',
    b'0',
    NULL,
    NULL,
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'agv_transfer_subflow_mock',
    1,
    's_agv_start_again',
    'DIRECT',
    'agv_start_again',
    'INSTRUMENT',
    1,
    NULL,
    '["s_open_door_for_agv"]',
    NULL,
    NULL,
    NULL,
    NULL,
    '{}',
    NULL,
    NULL,
    30000,
    1,
    2000,
    'FAIL_PIPELINE',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    b'1',
    NULL,
    4,
    '{"x":820,"y":160}',
    b'1',
    'ZONE-A',
    300000,
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
),
(
    'agv_transfer_subflow_mock',
    1,
    's_open_door_for_agv',
    'DIRECT',
    'door_open_for_agv',
    'INSTRUMENT',
    1,
    NULL,
    '["s_agv_wait_signal"]',
    NULL,
    NULL,
    NULL,
    NULL,
    '{}',
    NULL,
    NULL,
    30000,
    1,
    2000,
    'FAIL_PIPELINE',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    b'1',
    NULL,
    3,
    '{"x":620,"y":160}',
    b'1',
    'ZONE-A',
    300000,
    'admin',
    'admin',
    NOW(3),
    NOW(3),
    b'0'
);
