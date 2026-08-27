-- ============================================================================
-- 后处理机台 MOCK 配置样例
--
-- 目标：从设备 -> 设备指令 -> 步骤定义 -> 流程节点 -> 样本参数，跑通一个
--      “后处理机台启动 START” 的配置链路。
--
-- 使用前注意：
-- 1. 如果 lab_device_command 没有 completion_mode 字段，请先执行：
--      sql/alter_device_command_add_completion_mode.sql
-- 2. 当前 MOCK 驱动只返回 taskId，不会主动回调，所以 START 指令配置为 SYNC。
-- 3. 真实 IP / 接口暂未确定，因此设备 protocol 先使用 MOCK。
-- 4. ID 使用 910xxx 段，按你本地数据情况可自行调整。
-- ============================================================================

-- ────────────────────────────────────────────────────────────────────────────
-- 1. 设备响应解析规则：真实设备接入后使用
--    MOCK + SYNC 下暂时不会用到 codec，但先给出标准响应解析样例。
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO lab_device_codec (
    id, codec_name, parse_type, field_mapping, hex_rules, regex_rules,
    script_engine, script_content, unit_conversions, valid_range, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910001,
    '后处理机台-启动结果解析',
    'JSON',
    '{"deviceTaskId":"$.taskId","deviceStatus":"$.status","message":"$.message"}',
    NULL, NULL, NULL, NULL, NULL, NULL,
    '真实设备返回 JSON 时使用；MOCK 同步模式暂不依赖',
    'admin', NOW(), 'admin', NOW(), 0
);


-- ────────────────────────────────────────────────────────────────────────────
-- 2. 设备指令：后处理机台 START
--
-- request_template 说明：
-- - ${tray} / ${batchStationParams} / ${holeTasks} 是 Map/List，会被 ExpressionUtil
--   渲染成 JSON 对象/数组。
-- - executionId / nodeId / callbackToken / callbackUrl 即使不写，JSON body 也会被
--   DeviceGatewayImpl 自动注入；这里显式写出来是为了让设备联调时更直观。
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO lab_device_command (
    id, device_type, command_code, request_template, content_type,
    http_method, http_path, timeout_ms, retryable, codec_id, completion_mode,
    poll_path, poll_done_expr, poll_max_times, mock_output, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910101,
    'POST_PROCESSOR',
    'START',
    '{
      "schemaVersion": "${schemaVersion}",
      "taskType": "${taskType}",
      "sampleId": "${sampleId}",
      "tray": ${tray},
      "batchStationParams": ${batchStationParams},
      "holeTasks": ${holeTasks},
      "executionId": "${executionId}",
      "nodeId": "${nodeId}",
      "callbackToken": "${callbackToken}",
      "callbackUrl": "${callbackUrl}"
    }',
    'application/json',
    'POST',
    '/api/post-processor/start',
    600000,
    1,
    910001,
    'SYNC',
    NULL, NULL, NULL,
    '{"taskId":"MOCK-POST-PROCESSOR-START","status":"ACCEPTED","message":"mock start accepted"}',
    '后处理机台启动指令；真实接口/IP 未定，当前配合 MOCK + SYNC 使用',
    'admin', NOW(), 'admin', NOW(), 0
);

-- MOCK 心跳指令：用于 HeartbeatWatchdog 把 Redis 中的设备运行态恢复为 IDLE。
INSERT INTO lab_device_command (
    id, device_type, command_code, request_template, content_type,
    http_method, http_path, timeout_ms, retryable, codec_id, completion_mode,
    poll_path, poll_done_expr, poll_max_times, mock_output, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910102,
    'POST_PROCESSOR',
    'HEARTBEAT',
    '{"action":"ping"}',
    'application/json',
    'POST',
    '/api/post-processor/heartbeat',
    5000,
    0,
    NULL,
    'SYNC',
    NULL, NULL, NULL,
    '{"status":"OK"}',
    '后处理机台 MOCK 心跳指令；用于将运行态恢复为 IDLE',
    'admin', NOW(), 'admin', NOW(), 0
);


-- ────────────────────────────────────────────────────────────────────────────
-- 3. 设备信息：后处理机台 MOCK 设备
--
-- 真实设备接入时，把 protocol 改成 HTTP，并补充 base_url / host / port / auth_config。
-- 示例：
--   protocol = 'HTTP'
--   base_url = 'http://192.168.x.x:8080'
--   callback_mode = 'WEBHOOK'
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO lab_device_info (
    id, device_id, device_name, device_type, protocol, zone_code,
    host, port, base_url, callback_mode, poll_interval_ms,
    auth_type, auth_config,
    serial_port, baud_rate, data_bits, stop_bits, parity,
    sdk_class, sdk_config,
    connect_timeout_ms, read_timeout_ms,
    heartbeat_interval_ms, heartbeat_command,
    max_concurrent, status, enabled, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910201,
    'POST-PROCESSOR-MOCK-01',
    '后处理机台-MOCK',
    'POST_PROCESSOR',
    'MOCK',
    'ZONE-A',
    'localhost',
    0,
    NULL,
    'WEBHOOK',
    NULL,
    'NONE',
    NULL,
    NULL, NULL, NULL, NULL, NULL,
    NULL, NULL,
    5000,
    600000,
    30000,
    'HEARTBEAT',
    1,
    'ONLINE',
    1,
    '后处理机台 MOCK 设备；真实 IP 与接口确定后替换为 HTTP 设备',
    'admin', NOW(), 'admin', NOW(), 0
);


-- ────────────────────────────────────────────────────────────────────────────
-- 4. 资源配置：让 Flow 的 ResourcePool 能申请到 POST_PROCESSOR
--
-- 你的日志里出现：
--   ResourcePool 无候选资源 type=POST_PROCESSOR zone=ZONE-A reason=NO_CONFIG
-- 原因就是这里缺少 lab_resource_config。
--
-- 注意：
-- - resource_type 必须等于步骤的 device_type：POST_PROCESSOR
-- - zone_code 必须等于流程节点的 zone_code：ZONE-A
-- - resource_id 建议与 device_id 保持一致，方便排查。
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO lab_resource_config (
    id, resource_id, resource_type, ownership_type, zone_code,
    max_concurrent, enabled, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910251,
    'POST-PROCESSOR-MOCK-01',
    'POST_PROCESSOR',
    'EXCLUSIVE',
    'ZONE-A',
    1,
    b'1',
    '后处理机台 MOCK 资源；用于 Flow ResourcePool 申请',
    'admin', NOW(3), 'admin', NOW(3), b'0'
)
ON DUPLICATE KEY UPDATE
    resource_type = VALUES(resource_type),
    ownership_type = VALUES(ownership_type),
    zone_code = VALUES(zone_code),
    max_concurrent = VALUES(max_concurrent),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    updater = VALUES(updater),
    update_time = VALUES(update_time),
    deleted = b'0';


-- ────────────────────────────────────────────────────────────────────────────
-- 5. 步骤定义：后处理机台启动
--
-- 公共默认参数建议放这里：
-- - schemaVersion / taskType：任务参数结构约定
-- - tray：流程默认载具设置
-- - batchStationParams：工站默认公共参数
-- - holeTasks：默认空数组，实际任务由流程启动 inputParams 传入
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO pd_step_definition (
    id, step_key, version, name, description, step_type,
    device_type, command, executor, bean_name, method_name,
    chain_id, default_params, params_schema, output_fields,
    default_timeout_ms, default_max_attempts, default_backoff_ms,
    compensate_step_key, compensate_params,
    runnable_standalone, mock_output, status,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910301,
    'post_processor_start',
    1,
    '后处理机台启动',
    '下发后处理机台批处理启动任务，包含载具、工站公共参数和孔位任务参数',
    'INSTRUMENT',
    'POST_PROCESSOR',
    'START',
    NULL, NULL, NULL, NULL,
    '{
      "schemaVersion": "stationBatchTask.v1",
      "taskType": "stationBatchProcess",
      "tray": {
        "typeCode": "8ml_original_bottle",
        "address": "",
        "preserveSolid": true
      },
      "batchStationParams": {
        "quenching": {
          "enabled": true,
          "timeSec": 20,
          "tempC": 20.0,
          "rpm": 300
        },
        "recovery": {
          "enabled": true,
          "timeSec": 20,
          "tempC": 20.0,
          "rpm": 300
        },
        "nitrogenBlowing": {
          "enabled": true,
          "tempC": 40.0,
          "flow": 5.5,
          "steps": [
            {
              "index": 1,
              "enabled": false,
              "blow": false,
              "moveDistanceMm": 46,
              "moveTimeSec": 4,
              "dwellTimeSec": 0
            },
            {
              "index": 2,
              "enabled": true,
              "blow": true,
              "moveDistanceMm": 25,
              "moveTimeSec": 3,
              "dwellTimeSec": 4
            }
          ]
        }
      },
      "holeTasks": []
    }',
    '{
      "type": "object",
      "required": ["sampleId", "tray", "batchStationParams", "holeTasks"],
      "properties": {
        "sampleId": { "type": "string", "title": "样本编号" },
        "tray": { "type": "object", "title": "载具信息" },
        "batchStationParams": { "type": "object", "title": "批次工站公共参数" },
        "holeTasks": { "type": "array", "title": "孔位任务列表" }
      }
    }',
    '["deviceTaskId","deviceType","command"]',
    600000,
    1,
    5000,
    NULL,
    NULL,
    1,
    '{"deviceTaskId":"MOCK-POST-PROCESSOR-START","deviceType":"POST_PROCESSOR","command":"START"}',
    'ACTIVE',
    'admin', NOW(), 'admin', NOW(), 0
);


-- ────────────────────────────────────────────────────────────────────────────
-- 6. 流程定义：后处理批处理流程
--
-- 流程级默认入参建议放“流程公共默认值”，例如默认载具、默认工站参数。
-- 页面本次填写的批次参数在启动流程时通过 inputParams 覆盖。
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO pd_pipeline_definition (
    id, pipeline_key, version, name, description,
    fail_strategy, compensate_strategy,
    default_timeout_ms, default_max_attempts, default_backoff_ms,
    status, published_at, default_input_params, sample_mode, sample_bind_nodes,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910401,
    'post_processor_batch_mock',
    1,
    '后处理机台批处理流程-MOCK',
    '用于联调后处理机台启动参数下发；当前设备为 MOCK',
    'FAIL_FAST',
    'NONE',
    600000,
    1,
    5000,
    'ACTIVE',
    NOW(),
    '{
      "schemaVersion": "stationBatchTask.v1",
      "taskType": "stationBatchProcess",
      "tray": {
        "typeCode": "8ml_original_bottle",
        "address": "13",
        "preserveSolid": true
      },
      "batchStationParams": {
        "quenching": {
          "enabled": true,
          "timeSec": 20,
          "tempC": 20.0,
          "rpm": 300
        },
        "recovery": {
          "enabled": true,
          "timeSec": 20,
          "tempC": 20.0,
          "rpm": 300
        },
        "nitrogenBlowing": {
          "enabled": true,
          "tempC": 40.0,
          "flow": 5.5,
          "steps": [
            {
              "index": 1,
              "enabled": false,
              "blow": false,
              "moveDistanceMm": 46,
              "moveTimeSec": 4,
              "dwellTimeSec": 0
            },
            {
              "index": 2,
              "enabled": true,
              "blow": true,
              "moveDistanceMm": 25,
              "moveTimeSec": 3,
              "dwellTimeSec": 4
            }
          ]
        }
      },
      "holeTasks": []
    }',
    'REQUIRED',
    '["s_start_post_processor"]',
    'admin', NOW(), 'admin', NOW(), 0
);


-- ────────────────────────────────────────────────────────────────────────────
-- 7. 流程节点：启动后处理机台
--
-- input_mapping 说明：
-- - sampleId 来自流程启动参数或样本绑定参数。
-- - tray / batchStationParams / holeTasks 优先来自流程启动 inputParams。
-- - 如果启动时未传，则 StepSubmitter 会保留步骤 default_params 中的默认值。
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO pd_pipeline_step (
    id, pipeline_key, pipeline_version, node_id,
    dispatch_mode, task_type, step_type, step_key, step_version,
    depends_on, condition_expr, true_branch, false_branch, branches,
    params_override, input_mapping, output_mapping,
    timeout_ms, max_attempts, backoff_ms,
    on_failure, compensate_node_id, compensate_step_key,
    compensate_params, compensate_on,
    command, runnable_standalone, mock_output,
    sort_order, ui_position,
    resource_enabled, zone_code, resource_wait_timeout_ms,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910501,
    'post_processor_batch_mock',
    1,
    's_start_post_processor',
    'DIRECT',
    NULL,
    'INSTRUMENT',
    'post_processor_start',
    1,
    NULL, NULL, NULL, NULL, NULL,
    NULL,
    '{
      "sampleId": "${input.sampleId}",
      "tray": "${input.tray}",
      "batchStationParams": "${input.batchStationParams}",
      "holeTasks": "${input.holeTasks}"
    }',
    '{
      "deviceTaskId": "$.deviceTaskId",
      "deviceType": "$.deviceType",
      "command": "$.command"
    }',
    600000,
    1,
    5000,
    'FAIL_FAST',
    NULL, NULL, NULL, NULL,
    'START',
    1,
    NULL,
    1,
    '{"x":300,"y":120}',
    1,
    'ZONE-A',
    300000,
    'admin', NOW(), 'admin', NOW(), 0
);


-- ────────────────────────────────────────────────────────────────────────────
-- 8. 样本：样本长期实验参数
--
-- 这里放“样本自身长期绑定”的实验参数。
-- 本次页面临时选择的孔位任务、载具位置、批次公共参数，不建议长期写死在样本里；
-- 建议启动流程时通过 inputParams 传入。
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO lab_sample_info (
    id, sample_id, parent_sample_id, root_sample_id,
    derive_type, generation, sample_type, sample_name,
    container_type, container_code,
    volume_ul, initial_volume_ul, concentration,
    attributes, experiment_params,
    status, location_code, location_detail,
    current_execution_id, current_node_id,
    batch_no, order_no, priority, source, external_id,
    collected_at, received_at, expire_time, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    910601,
    'SAMPLE-POST-001',
    NULL,
    'SAMPLE-POST-001',
    'ORIGINAL',
    0,
    'MIXTURE',
    '后处理测试样本001',
    'VIAL',
    'BC-SAMPLE-POST-001',
    1000.000000,
    1000.000000,
    NULL,
    '{"color":"unknown","source":"manual-test"}',
    '{
      "preferredHoleNo": 1,
      "sampleProcessProfile": "default_post_process",
      "targetRecoveryVolumeUl": 1000.0,
      "remark": "样本自身参数示例；本次孔位任务仍建议由启动 inputParams 传入"
    }',
    'REGISTERED',
    NULL,
    NULL,
    NULL,
    NULL,
    'BATCH-POST-MOCK-001',
    'ORDER-POST-MOCK-001',
    0,
    'MANUAL',
    NULL,
    NOW(),
    NOW(),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    '后处理机台 MOCK 流程样本',
    'admin', NOW(), 'admin', NOW(), 0
);


-- ────────────────────────────────────────────────────────────────────────────
-- 9. 启动流程时建议传入的 inputParams 样例
--
-- 这个不是 INSERT，只是给你调用启动接口时复制使用：
--
-- POST /flow/pipeline-execution/start 或项目内对应启动接口
-- pipelineKey = post_processor_batch_mock
-- pipelineVersion = 1
-- inputParams = 下方 JSON
-- ────────────────────────────────────────────────────────────────────────────

/*
{
  "sampleId": "SAMPLE-POST-001",
  "tray": {
    "typeCode": "8ml_original_bottle",
    "address": "13",
    "preserveSolid": true
  },
  "batchStationParams": {
    "quenching": {
      "enabled": true,
      "timeSec": 20,
      "tempC": 20.0,
      "rpm": 300
    },
    "recovery": {
      "enabled": true,
      "timeSec": 20,
      "tempC": 20.0,
      "rpm": 300
    },
    "nitrogenBlowing": {
      "enabled": true,
      "tempC": 40.0,
      "flow": 5.5,
      "steps": [
        {
          "index": 1,
          "enabled": false,
          "blow": false,
          "moveDistanceMm": 46,
          "moveTimeSec": 4,
          "dwellTimeSec": 0
        },
        {
          "index": 2,
          "enabled": true,
          "blow": true,
          "moveDistanceMm": 25,
          "moveTimeSec": 3,
          "dwellTimeSec": 4
        }
      ]
    }
  },
  "holeTasks": [
    {
      "holeNo": 1,
      "sampleId": "SAMPLE-POST-001",
      "emptyTask": false,
      "parameterSource": "hole",
      "stationTasks": {
        "quenching": {
          "tiltTimeSec": 15,
          "liquidSteps": [
            {
              "index": 1,
              "action": "quench",
              "liquidCode": "test1",
              "volumeUl": 100.0
            },
            {
              "index": 2,
              "action": "wash",
              "liquidCode": "test1",
              "volumeUl": 100.0
            },
            {
              "index": 3,
              "action": "wash",
              "liquidCode": "test1",
              "volumeUl": 10.0
            }
          ]
        },
        "recovery": {
          "nitrogenBlowStepCount": 2,
          "liquidSteps": [
            {
              "index": 1,
              "action": "recovery",
              "liquidCode": "test2",
              "volumeUl": 1000.0
            }
          ]
        }
      }
    }
  ]
}
*/
