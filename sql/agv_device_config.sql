SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ================================================================
-- AGV device config & command templates
-- Replace 192.168.1.200:8080 with the real AGV server address
-- ================================================================

-- ----------------------------------------------------------------
-- 1. AGV device registration
-- ----------------------------------------------------------------
INSERT INTO lab_device_info (
    device_id, device_name, device_type, protocol,
    base_url, http_method,
    callback_mode,
    connect_timeout_ms, read_timeout_ms,
    max_concurrent, enabled,
    remark,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'AGV-01', 'AGV-01', 'AGV', 'HTTP',
    'http://192.168.1.200:8080', 'POST',
    'WEBHOOK',
    5000, 60000,
    3, b'1',
    'AGV-01',
    'system', 'system', NOW(3), NOW(3), b'0'
) ON DUPLICATE KEY UPDATE
    device_name        = VALUES(device_name),
    base_url           = VALUES(base_url),
    remark             = VALUES(remark),
    update_time        = NOW(3);

-- ----------------------------------------------------------------
-- 2. Command templates
--    System auto-injects: taskId(=executionId), executionId, nodeId, callbackToken
-- ----------------------------------------------------------------

-- AGV_STEP1: arm picks from source slot onto AGV
INSERT INTO lab_device_command (
    device_type, command_code,
    http_method, http_path,
    request_template,
    completion_mode, timeout_ms, retryable,
    remark,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'AGV', 'AGV_STEP1',
    'POST', '/device/agv/startTask',
    '{
  "taskId":    "${taskId}",
  "taskType":  ${taskType},
  "plateType": "${plateType}",
  "step1": {
    "sourceArea":     "${sourceArea}",
    "sourcePos":      "${sourcePos}",
    "sourceStartPos": "${sourceStartPos}",
    "count":          ${count}
  }
}',
    'WEBHOOK', 120000, false,
    'AGV_STEP1: arm picks from source slot onto AGV',
    'system', 'system', NOW(3), NOW(3), b'0'
) ON DUPLICATE KEY UPDATE
    request_template = VALUES(request_template),
    remark           = VALUES(remark),
    update_time      = NOW(3);

-- AGV_STEP2: AGV moves to destination station
INSERT INTO lab_device_command (
    device_type, command_code,
    http_method, http_path,
    request_template,
    completion_mode, timeout_ms, retryable,
    remark,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'AGV', 'AGV_STEP2',
    'POST', '/device/agv/startTask',
    '{
  "taskId": "${taskId}",
  "step2": {
    "agvStationName": "${agvStationName}"
  }
}',
    'WEBHOOK', 180000, false,
    'AGV_STEP2: AGV moves to destination station',
    'system', 'system', NOW(3), NOW(3), b'0'
) ON DUPLICATE KEY UPDATE
    request_template = VALUES(request_template),
    remark           = VALUES(remark),
    update_time      = NOW(3);

-- AGV_STEP3: arm picks from AGV onto target slot
INSERT INTO lab_device_command (
    device_type, command_code,
    http_method, http_path,
    request_template,
    completion_mode, timeout_ms, retryable,
    remark,
    creator, updater, create_time, update_time, deleted
) VALUES (
    'AGV', 'AGV_STEP3',
    'POST', '/device/agv/startTask',
    '{
  "taskId":    "${taskId}",
  "taskType":  ${taskType},
  "plateType": "${plateType}",
  "step3": {
    "desArea":             "${desArea}",
    "desPos":              "${desPos}",
    "destinationStartPos": "${destinationStartPos}"
  }
}',
    'WEBHOOK', 120000, false,
    'AGV_STEP3: arm picks from AGV onto target slot',
    'system', 'system', NOW(3), NOW(3), b'0'
) ON DUPLICATE KEY UPDATE
    request_template = VALUES(request_template),
    remark           = VALUES(remark),
    update_time      = NOW(3);

-- ----------------------------------------------------------------
-- 3. Resource config (add AGV-01 to scheduler pool)
-- ----------------------------------------------------------------
INSERT INTO lab_resource_config (
    resource_id, resource_type, ownership_type,
    zone_code, max_concurrent, enabled,
    remark, creator, updater, create_time, update_time, deleted
) VALUES (
    'AGV-01', 'AGV', 'SHARED',
    NULL, 1, b'1',
    'AGV-01', 'system', 'system', NOW(3), NOW(3), b'0'
) ON DUPLICATE KEY UPDATE
    enabled     = b'1',
    remark      = VALUES(remark),
    update_time = NOW(3);
