-- ============================================================================
-- 后处理机台最终驼峰参数结构更新 SQL
--
-- 目标：
-- 1. DeviceCommand.request_template 输出最终确认的驼峰下发体：
--      callbackToken / callbackUrl / executionId / nodeId / tasks
-- 2. StepDefinition.default_params 提供默认 sampleId + tasks。
-- 3. PipelineDefinition.default_input_params 提供流程启动默认 sampleId + tasks。
-- 4. PipelineStep.input_mapping 从 input 中取 sampleId + tasks。
--
-- 注意：
-- - tasks 是数组，ExpressionUtil.render 会把 List/Map 渲染成 JSON。
-- - callbackToken / callbackUrl / executionId / nodeId 由 StepSubmitter 生成。
-- - MOCK 验证时 completion_mode 使用 SYNC。
-- ============================================================================

UPDATE lab_device_command
SET request_template = '{
  "callbackToken": "${callbackToken}",
  "callbackUrl": "${callbackUrl}",
  "executionId": "${executionId}",
  "nodeId": "${nodeId}",
  "tasks": ${tasks}
}',
    content_type = 'application/json',
    http_method = 'POST',
    http_path = '/api/post-processor/start',
    timeout_ms = 600000,
    retryable = 1,
    completion_mode = 'SYNC',
    remark = '后处理机台启动指令；最终驼峰参数结构，MOCK + SYNC',
    updater = 'admin',
    update_time = NOW(),
    deleted = 0
WHERE device_type = 'POST_PROCESSOR'
  AND command_code = 'START';


UPDATE pd_step_definition
SET default_params = '{
  "sampleId": "SAMPLE-POST-001",
  "tasks": [
    {
      "stations": [
        {
          "stationCode": "quench",
          "tempC": 20.0,
          "timeSec": 20,
          "rpm": 300,
          "flowAir": 0,
          "steps": []
        },
        {
          "stationCode": "recovery",
          "tempC": 20.0,
          "timeSec": 20,
          "rpm": 300,
          "flowAir": 0,
          "steps": []
        },
        {
          "stationCode": "nitrogenBlowing",
          "tempC": 20.0,
          "timeSec": 0,
          "rpm": 0,
          "flowAir": 30,
          "steps": [
            {
              "index": 1,
              "blowAir": false,
              "moveLengthMm": 45,
              "moveTimeSec": 5,
              "waitTimeSec": 0
            },
            {
              "index": 2,
              "blowAir": true,
              "moveLengthMm": 20,
              "moveTimeSec": 30,
              "waitTimeSec": 20
            }
          ]
        }
      ],
      "trayInfo": {
        "typeCode": "1",
        "address": "13",
        "needRetain": true
      },
      "sampleInfos": [
        {
          "hole": 1,
          "sampleId": "12",
          "filterTimeSec": 10,
          "nitrogenBlowingStepCount": 2,
          "steps": [
            {
              "index": 1,
              "stepType": "quench",
              "solventCode": "1",
              "volumeUl": 1000
            },
            {
              "index": 2,
              "stepType": "wash",
              "solventCode": "1",
              "volumeUl": 1000
            },
            {
              "index": 3,
              "stepType": "recovery",
              "solventCode": "1",
              "volumeUl": 1000
            }
          ]
        }
      ]
    }
  ]
}',
    params_schema = '{
  "type": "object",
  "required": ["sampleId", "tasks"],
  "properties": {
    "sampleId": { "type": "string", "title": "流程样本编号" },
    "tasks": {
      "type": "array",
      "title": "后处理任务参数列表",
      "items": {
        "type": "object",
        "required": ["stations", "trayInfo", "sampleInfos"]
      }
    }
  }
}',
    output_fields = '["deviceTaskId","deviceType","command"]',
    updater = 'admin',
    update_time = NOW(),
    deleted = 0
WHERE step_key = 'post_processor_start'
  AND version = 1;


UPDATE pd_pipeline_definition
SET default_input_params = '{
  "sampleId": "SAMPLE-POST-001",
  "tasks": [
    {
      "stations": [
        {
          "stationCode": "quench",
          "tempC": 20.0,
          "timeSec": 20,
          "rpm": 300,
          "flowAir": 0,
          "steps": []
        },
        {
          "stationCode": "recovery",
          "tempC": 20.0,
          "timeSec": 20,
          "rpm": 300,
          "flowAir": 0,
          "steps": []
        },
        {
          "stationCode": "nitrogenBlowing",
          "tempC": 20.0,
          "timeSec": 0,
          "rpm": 0,
          "flowAir": 30,
          "steps": [
            {
              "index": 1,
              "blowAir": false,
              "moveLengthMm": 45,
              "moveTimeSec": 5,
              "waitTimeSec": 0
            },
            {
              "index": 2,
              "blowAir": true,
              "moveLengthMm": 20,
              "moveTimeSec": 30,
              "waitTimeSec": 20
            }
          ]
        }
      ],
      "trayInfo": {
        "typeCode": "1",
        "address": "13",
        "needRetain": true
      },
      "sampleInfos": [
        {
          "hole": 1,
          "sampleId": "12",
          "filterTimeSec": 10,
          "nitrogenBlowingStepCount": 2,
          "steps": [
            {
              "index": 1,
              "stepType": "quench",
              "solventCode": "1",
              "volumeUl": 1000
            },
            {
              "index": 2,
              "stepType": "wash",
              "solventCode": "1",
              "volumeUl": 1000
            },
            {
              "index": 3,
              "stepType": "recovery",
              "solventCode": "1",
              "volumeUl": 1000
            }
          ]
        }
      ]
    }
  ]
}',
    updater = 'admin',
    update_time = NOW(),
    deleted = 0
WHERE pipeline_key = 'post_processor_batch_mock'
  AND version = 1;


UPDATE pd_pipeline_step
SET input_mapping = '{
  "sampleId": "${input.sampleId}",
  "tasks": "${input.tasks}"
}',
    output_mapping = '{
  "deviceTaskId": "$.deviceTaskId",
  "deviceType": "$.deviceType",
  "command": "$.command"
}',
    resource_enabled = b'1',
    zone_code = 'ZONE-A',
    command = 'START',
    updater = 'admin',
    update_time = NOW(),
    deleted = 0
WHERE pipeline_key = 'post_processor_batch_mock'
  AND pipeline_version = 1
  AND node_id = 's_start_post_processor';


-- 资源和设备健康配置兜底，避免再次出现 NO_CONFIG / NO_HEALTHY。
INSERT INTO lab_resource_config (
    resource_id, resource_type, ownership_type, zone_code,
    max_concurrent, enabled, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
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

UPDATE lab_device_info
SET protocol = 'MOCK',
    zone_code = 'ZONE-A',
    heartbeat_interval_ms = 30000,
    heartbeat_command = 'HEARTBEAT',
    enabled = b'1',
    status = 'ONLINE',
    updater = 'admin',
    update_time = NOW()
WHERE device_id = 'POST-PROCESSOR-MOCK-01';

