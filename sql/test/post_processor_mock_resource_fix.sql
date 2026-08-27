-- ============================================================================
-- 后处理机台 MOCK 资源修复 SQL
--
-- 适用场景：
-- 日志出现：
--   [ResourcePool] 无候选资源 type=POST_PROCESSOR zone=ZONE-A reason=NO_CONFIG
--
-- 原因：
-- 流程节点 s_start_post_processor 配置了 resource_enabled=1，
-- StepSubmitter 会先向 ResourcePool 申请 POST_PROCESSOR / ZONE-A 资源。
-- 如果 lab_resource_config 没有对应记录，流程会停在 PENDING 等待资源。
--
-- 执行后：
-- 重新启动一次 post_processor_batch_mock 流程即可。
-- ============================================================================

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

-- 补 MOCK 心跳指令。HeartbeatWatchdog 成功执行后，会把 Redis 设备状态标记为 IDLE。
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
)
ON DUPLICATE KEY UPDATE
    request_template = VALUES(request_template),
    content_type = VALUES(content_type),
    http_method = VALUES(http_method),
    http_path = VALUES(http_path),
    timeout_ms = VALUES(timeout_ms),
    retryable = VALUES(retryable),
    completion_mode = VALUES(completion_mode),
    mock_output = VALUES(mock_output),
    remark = VALUES(remark),
    updater = VALUES(updater),
    update_time = VALUES(update_time),
    deleted = 0;

-- 让 MOCK 设备参与心跳。注意：这不是立即生效为 IDLE，立即生效需要写 Redis；
-- 本次我已通过 Redis 写入 device:state:POST-PROCESSOR-MOCK-01 status=IDLE。
UPDATE lab_device_info
SET heartbeat_interval_ms = 30000,
    heartbeat_command = 'HEARTBEAT',
    enabled = b'1',
    status = 'ONLINE',
    updater = 'admin',
    update_time = NOW()
WHERE device_id = 'POST-PROCESSOR-MOCK-01';
