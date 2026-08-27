-- ============================================================================
-- 后处理机台配置恢复 SQL
-- 覆盖范围：设备信息、设备指令、步骤定义、流程定义、流程节点、资源配置
-- 报文结构：tasks 数组（与 post_processor_final_payload_update.sql 对齐）
-- ============================================================================

-- ── 1. 设备指令 ──────────────────────────────────────────────────────────────

INSERT INTO lab_device_command (
    device_type, command_code, request_template, content_type,
    http_method, http_path, timeout_ms, retryable, completion_mode,
    mock_output, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    'POST_PROCESSOR',
    'START',
    '{
  "callbackToken": "${callbackToken}",
  "callbackUrl": "${callbackUrl}",
  "executionId": "${executionId}",
  "nodeId": "${nodeId}",
  "tasks": ${tasks}
}',
    'application/json',
    'POST',
    '/api/post-processor/start',
    600000,
    1,
    'SYNC',
    '{"code":0,"msg":"mock start accepted"}',
    '后处理机台启动指令；tasks 数组透传，系统自动注入 callbackToken/callbackUrl/executionId/nodeId',
    'admin', NOW(3), 'admin', NOW(3), 0
),
(
    'POST_PROCESSOR',
    'HEARTBEAT',
    '{"action":"ping"}',
    'application/json',
    'POST',
    '/api/post-processor/heartbeat',
    5000,
    0,
    'SYNC',
    '{"status":"OK"}',
    '后处理机台心跳指令',
    'admin', NOW(3), 'admin', NOW(3), 0
);

-- ── 2. 设备信息 ──────────────────────────────────────────────────────────────

INSERT INTO lab_device_info (
    device_id, device_name, device_type, protocol, zone_code,
    host, port, base_url, callback_mode,
    auth_type, connect_timeout_ms, read_timeout_ms,
    heartbeat_interval_ms, heartbeat_command,
    max_concurrent, status, enabled, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    'POST-PROCESSOR-MOCK-01',
    '后处理机台-MOCK',
    'POST_PROCESSOR',
    'MOCK',
    'ZONE-A',
    'localhost', 0, NULL,
    'WEBHOOK',
    'NONE', 5000, 600000,
    30000, 'HEARTBEAT',
    1, 'ONLINE', 1,
    '后处理机台 MOCK 设备；真实 IP 确定后改 protocol=HTTP 并填 base_url',
    'admin', NOW(3), 'admin', NOW(3), 0
);

-- ── 3. 资源配置（已存在则更新）────────────────────────────────────────────────

INSERT INTO lab_resource_config (
    resource_id, resource_type, ownership_type, zone_code,
    max_concurrent, enabled, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    'POST-PROCESSOR-MOCK-01',
    'POST_PROCESSOR',
    'EXCLUSIVE',
    'ZONE-A',
    1, b'1',
    '后处理机台 MOCK 资源；Flow ResourcePool 申请使用',
    'admin', NOW(3), 'admin', NOW(3), b'0'
)
ON DUPLICATE KEY UPDATE
    enabled      = b'1',
    updater      = 'admin',
    update_time  = NOW(3),
    deleted      = b'0';

-- ── 4. 步骤定义 ──────────────────────────────────────────────────────────────

INSERT INTO pd_step_definition (
    step_key, version, name, description, step_type,
    device_type, command,
    default_params, params_schema, output_fields,
    default_timeout_ms, default_max_attempts, default_backoff_ms,
    runnable_standalone, status,
    creator, create_time, updater, update_time, deleted
) VALUES (
    'post_processor_start',
    1,
    '后处理机台启动',
    '下发后处理机台批处理启动任务，tasks 数组透传给设备',
    'INSTRUMENT',
    'POST_PROCESSOR',
    'START',
    '{
  "tasks": [
    {
      "stations": [
        {"stationCode":"quench","tempC":20,"timeSec":20,"rpm":300,"flowAir":0,"steps":[]},
        {"stationCode":"recovery","tempC":20,"timeSec":20,"rpm":300,"flowAir":0,"steps":[]},
        {"stationCode":"nitrogenBlowing","tempC":20,"timeSec":0,"rpm":0,"flowAir":30,"steps":[
          {"index":1,"blowAir":false,"moveLengthMm":45,"moveTimeSec":5,"waitTimeSec":0},
          {"index":2,"blowAir":true,"moveLengthMm":20,"moveTimeSec":30,"waitTimeSec":20}
        ]}
      ],
      "trayInfo": {"typeCode":"1","address":"13","needRetain":true},
      "sampleInfos": [
        {
          "hole":1,"sampleId":"12","filterTimeSec":10,"nitrogenBlowingStepCount":2,
          "steps":[
            {"index":1,"stepType":"quench","solventCode":"1","volumeUl":1000},
            {"index":2,"stepType":"wash","solventCode":"1","volumeUl":1000},
            {"index":3,"stepType":"recovery","solventCode":"1","volumeUl":1000}
          ]
        }
      ]
    }
  ]
}',
    '{"type":"object","required":["tasks"],"properties":{"tasks":{"type":"array","title":"后处理任务列表"}}}',
    '["deviceTaskId","deviceType","command"]',
    600000, 1, 0,
    1, 'ACTIVE',
    'admin', NOW(3), 'admin', NOW(3), 0
);

-- ── 5. 流程定义 ──────────────────────────────────────────────────────────────

INSERT INTO pd_pipeline_definition (
    pipeline_key, version, name, description, status,
    default_input_params,
    creator, create_time, updater, update_time, deleted
) VALUES (
    'post_processor_batch_mock',
    1,
    '后处理机台批处理流程',
    '单步流程：启动后处理机台 START，MOCK 模式同步返回',
    'ACTIVE',
    '{
  "tasks": [
    {
      "stations": [
        {"stationCode":"quench","tempC":20,"timeSec":20,"rpm":300,"flowAir":0,"steps":[]},
        {"stationCode":"recovery","tempC":20,"timeSec":20,"rpm":300,"flowAir":0,"steps":[]},
        {"stationCode":"nitrogenBlowing","tempC":20,"timeSec":0,"rpm":0,"flowAir":30,"steps":[
          {"index":1,"blowAir":false,"moveLengthMm":45,"moveTimeSec":5,"waitTimeSec":0},
          {"index":2,"blowAir":true,"moveLengthMm":20,"moveTimeSec":30,"waitTimeSec":20}
        ]}
      ],
      "trayInfo": {"typeCode":"1","address":"13","needRetain":true},
      "sampleInfos": [
        {
          "hole":1,"sampleId":"12","filterTimeSec":10,"nitrogenBlowingStepCount":2,
          "steps":[
            {"index":1,"stepType":"quench","solventCode":"1","volumeUl":1000},
            {"index":2,"stepType":"wash","solventCode":"1","volumeUl":1000},
            {"index":3,"stepType":"recovery","solventCode":"1","volumeUl":1000}
          ]
        }
      ]
    }
  ]
}',
    'admin', NOW(3), 'admin', NOW(3), 0
);

-- ── 6. 流程节点 ──────────────────────────────────────────────────────────────

INSERT INTO pd_pipeline_step (
    pipeline_key, pipeline_version, node_id, dispatch_mode,
    step_key, step_version,
    input_mapping, output_mapping,
    resource_enabled, zone_code, command,
    creator, create_time, updater, update_time, deleted
) VALUES (
    'post_processor_batch_mock',
    1,
    's_start_post_processor',
    'DIRECT',
    'post_processor_start',
    1,
    '{"tasks": "${input.tasks}"}',
    '{"deviceTaskId":"$.deviceTaskId","deviceType":"$.deviceType","command":"$.command"}',
    b'1',
    'ZONE-A',
    'START',
    'admin', NOW(3), 'admin', NOW(3), 0
);
