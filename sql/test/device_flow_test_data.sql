-- ============================================================================
-- 设备模块 + 流程引擎 完整测试数据
--
-- 场景：水质检测流程
--   1. 采样登记 (COMPUTE)
--   2. 并行：PH检测 (INSTRUMENT) + 浊度检测 (INSTRUMENT)
--   3. 数据汇算 (COMPUTE) — 汇合并行结果
--   4. 条件分支 (CONDITION) — 根据 score 分级
--      ├─ GRADE_A → 直接归档 (COMPUTE)
--      ├─ GRADE_B → 人工审核 (WAIT) → 归档
--      └─ default → 发送复检任务 (QUEUED/TASK) → 归档
--   5. 通知 (NOTIFY)
--
-- 包含：设备定义、设备指令、设备解析规则、步骤定义、流程定义、流程编排
-- ============================================================================

-- ────────────────────────────────────────────────────────────────────────────
-- 1. 设备解析规则 (lab_device_codec)
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO lab_device_codec (id, codec_name, parse_type, field_mapping, hex_rules, regex_rules,
                              script_engine, script_content, unit_conversions, valid_range, remark,
                              creator, create_time, updater, update_time, deleted)
VALUES
-- PH计 JSON 解析：从 {"result":{"phValue":7.35,"tempValue":25.0}} 提取
(1001, 'PH计-测量结果解析', 'JSON',
 '{"ph":"$.result.phValue","temperature":"$.result.tempValue"}',
 NULL, NULL, NULL, NULL,
 NULL,
 '{"ph":{"min":0,"max":14},"temperature":{"min":-10,"max":100}}',
 'PH计标准JSON响应解析',
 'admin', NOW(), 'admin', NOW(), 0),

-- 浊度计 JSON 解析：从 {"ntu":12.5,"status":"OK"} 提取
(1002, '浊度计-测量结果解析', 'JSON',
 '{"turbidity":"$.ntu","deviceStatus":"$.status"}',
 NULL, NULL, NULL, NULL,
 NULL,
 '{"turbidity":{"min":0,"max":4000}}',
 '浊度计标准JSON响应解析',
 'admin', NOW(), 'admin', NOW(), 0);


-- ────────────────────────────────────────────────────────────────────────────
-- 2. 设备指令 (lab_device_command)
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO lab_device_command (id, device_type, command_code, request_template, content_type,
                                http_method, http_path, timeout_ms, retryable, codec_id,
                                poll_path, poll_done_expr, poll_max_times, remark,
                                creator, create_time, updater, update_time, deleted)
VALUES
-- PH计 - 测量指令
(2001, 'PH_METER', 'MEASURE',
 '{"sampleId":"${sampleId}","action":"measure"}',
 'application/json', 'POST', '/api/v1/measure',
 30000, 1, 1001,
 NULL, NULL, NULL,
 'PH计测量指令',
 'admin', NOW(), 'admin', NOW(), 0),

-- PH计 - 心跳指令
(2002, 'PH_METER', 'HEARTBEAT',
 '{"action":"ping"}',
 'application/json', 'GET', '/api/v1/health',
 5000, 0, NULL,
 NULL, NULL, NULL,
 'PH计心跳检测',
 'admin', NOW(), 'admin', NOW(), 0),

-- 浊度计 - 测量指令（POLL模式，需要轮询结果）
(2003, 'TURBIDITY_METER', 'MEASURE',
 '{"sampleId":"${sampleId}","action":"measure"}',
 'application/json', 'POST', '/api/v1/measure',
 60000, 1, 1002,
 '/api/v1/result/${taskId}', '$.status == "DONE"', 20,
 '浊度计测量指令（需轮询结果）',
 'admin', NOW(), 'admin', NOW(), 0),

-- 浊度计 - 心跳指令
(2004, 'TURBIDITY_METER', 'HEARTBEAT',
 '{"action":"ping"}',
 'application/json', 'GET', '/api/v1/health',
 5000, 0, NULL,
 NULL, NULL, NULL,
 '浊度计心跳检测',
 'admin', NOW(), 'admin', NOW(), 0);


-- ────────────────────────────────────────────────────────────────────────────
-- 3. 设备信息 (lab_device_info)
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO lab_device_info (id, device_id, device_name, device_type, protocol, zone_code,
                             host, port, base_url, callback_mode, poll_interval_ms,
                             auth_type, auth_config,
                             serial_port, baud_rate, data_bits, stop_bits, parity,
                             sdk_class, sdk_config,
                             connect_timeout_ms, read_timeout_ms,
                             heartbeat_interval_ms, heartbeat_command,
                             max_concurrent, status, enabled, remark,
                             creator, create_time, updater, update_time, deleted)
VALUES
-- PH计 1号 — HTTP + WEBHOOK 回调
(3001, 'PH-METER-01', '雷磁PH计1号', 'PH_METER', 'HTTP', 'ZONE-A',
 '192.168.1.101', 8080, 'http://192.168.1.101:8080', 'WEBHOOK', NULL,
 'TOKEN', '{"token":"ph-device-token-001","headerName":"Authorization","prefix":"Bearer "}',
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL,
 5000, 10000,
 60000, 'HEARTBEAT',
 1, 'ONLINE', 1, '一楼A区PH检测仪',
 'admin', NOW(), 'admin', NOW(), 0),

-- PH计 2号 — HTTP + WEBHOOK 回调（同类型备用设备，用于测试轮询选择）
(3002, 'PH-METER-02', '雷磁PH计2号', 'PH_METER', 'HTTP', 'ZONE-A',
 '192.168.1.102', 8080, 'http://192.168.1.102:8080', 'WEBHOOK', NULL,
 'TOKEN', '{"token":"ph-device-token-002","headerName":"Authorization","prefix":"Bearer "}',
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL,
 5000, 10000,
 60000, 'HEARTBEAT',
 1, 'ONLINE', 1, '一楼A区PH检测仪备用',
 'admin', NOW(), 'admin', NOW(), 0),

-- 浊度计 1号 — HTTP + POLL 模式
(3003, 'TURBIDITY-01', '哈希浊度计1号', 'TURBIDITY_METER', 'HTTP', 'ZONE-A',
 '192.168.1.201', 8080, 'http://192.168.1.201:8080', 'POLL', 3000,
 'BASIC', '{"username":"admin","password":"turbidity123"}',
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL,
 5000, 15000,
 60000, 'HEARTBEAT',
 1, 'ONLINE', 1, '一楼A区浊度检测仪',
 'admin', NOW(), 'admin', NOW(), 0),

-- MOCK 设备（开发调试用）
(3004, 'MOCK-DEVICE-01', 'MOCK测试设备', 'PH_METER', 'MOCK', 'ZONE-A',
 'localhost', 0, NULL, 'WEBHOOK', NULL,
 'NONE', NULL,
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL,
 5000, 5000,
 0, NULL,
 1, 'ONLINE', 1, 'Mock设备，开发调试用',
 'admin', NOW(), 'admin', NOW(), 0);


-- ────────────────────────────────────────────────────────────────────────────
-- 4. 步骤定义 (pd_step_definition)
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO pd_step_definition (id, step_key, version, name, description, step_type,
                                 device_type, command, executor, bean_name, method_name,
                                 chain_id, default_params, params_schema, output_fields,
                                 default_timeout_ms, default_max_attempts, default_backoff_ms,
                                 compensate_step_key, compensate_params,
                                 runnable_standalone, mock_output, status,
                                 creator, create_time, updater, update_time, deleted)
VALUES
-- ① 采样登记（计算节点）
(4001, 'sample_register', 1, '采样登记', '录入样品基本信息并分配样品ID',
 'COMPUTE', NULL, NULL,
 'BEAN', 'sampleRegisterBean', 'register', NULL,
 '{"source":"incoming"}',
 '{"type":"object","properties":{"source":{"type":"string"}}}',
 '["sampleId","sampleName","source"]',
 10000, 1, NULL,
 NULL, NULL,
 1, '{"sampleId":"SAMPLE-TEST-001","sampleName":"测试样品","source":"incoming"}',
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0),

-- ② PH检测（仪器节点）
(4002, 'ph_measure', 1, 'PH检测', 'PH计检测样品酸碱度',
 'INSTRUMENT', 'PH_METER', 'MEASURE',
 NULL, NULL, NULL, NULL,
 '{}', NULL,
 '["ph","temperature"]',
 30000, 2, 5000,
 NULL, NULL,
 1, '{"ph":7.35,"temperature":25.0}',
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0),

-- ③ 浊度检测（仪器节点）
(4003, 'turbidity_measure', 1, '浊度检测', '浊度计检测样品浑浊度',
 'INSTRUMENT', 'TURBIDITY_METER', 'MEASURE',
 NULL, NULL, NULL, NULL,
 '{}', NULL,
 '["turbidity"]',
 60000, 2, 5000,
 NULL, NULL,
 1, '{"turbidity":12.5}',
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0),

-- ④ 数据汇算（计算节点）
(4004, 'score_calc', 1, '水质评分计算', '汇总PH和浊度数据，计算综合评分',
 'COMPUTE', NULL, NULL,
 'BEAN', 'waterQualityCalcBean', 'calculate', NULL,
 '{}', NULL,
 '["score","grade","qualified"]',
 10000, 1, NULL,
 NULL, NULL,
 1, '{"score":85,"grade":"GRADE_A","qualified":true}',
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑤ 条件判断（条件节点）
(4005, 'quality_branch', 1, '水质分级', '根据综合评分决定后续处理路径',
 'CONDITION', NULL, NULL,
 NULL, NULL, NULL, NULL,
 '{}', NULL,
 '["branchName","conditionResult"]',
 5000, 1, NULL,
 NULL, NULL,
 0, NULL,
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑥ 直接归档（计算节点）
(4006, 'archive', 1, '结果归档', '将检测结果归档入库',
 'COMPUTE', NULL, NULL,
 'BEAN', 'archiveBean', 'archive', NULL,
 '{}', NULL,
 '["archiveId","archivedAt"]',
 10000, 1, NULL,
 NULL, NULL,
 1, '{"archiveId":"ARC-001","archivedAt":"2026-05-08T10:00:00"}',
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑦ 人工审核（等待节点）
(4007, 'manual_review', 1, '人工审核', '中等水质需要人工确认是否合格',
 'WAIT', NULL, NULL,
 NULL, NULL, NULL, NULL,
 '{"waitReason":"水质评分中等，需人工确认","assignee":"quality_inspector"}',
 NULL,
 '["approved","reviewComment"]',
 86400000, 1, NULL,
 NULL, NULL,
 0, NULL,
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑧ 复检任务（计算节点 — 实际场景中可走 QUEUED 模式下发任务）
(4008, 'retest_task', 1, '下发复检任务', '水质不合格，生成复检工单并下发',
 'COMPUTE', NULL, NULL,
 'BEAN', 'retestTaskBean', 'createTask', NULL,
 '{"reason":"water_quality_failed"}',
 NULL,
 '["taskId","taskStatus"]',
 30000, 1, NULL,
 NULL, NULL,
 0, '{"taskId":"TASK-RETEST-001","taskStatus":"CREATED"}',
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑨ 通知（通知节点）
(4009, 'result_notify', 1, '检测结果通知', '将检测结果通知相关人员',
 'NOTIFY', NULL, NULL,
 NULL, NULL, NULL, NULL,
 '{"channel":"DING_TALK","templateCode":"water_quality_result"}',
 NULL,
 '["notified"]',
 10000, 1, NULL,
 NULL, NULL,
 0, '{"notified":true}',
 'ACTIVE',
 'admin', NOW(), 'admin', NOW(), 0);


-- ────────────────────────────────────────────────────────────────────────────
-- 5. 流程定义 (pd_pipeline_definition)
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO pd_pipeline_definition (id, pipeline_key, version, name, description,
                                     fail_strategy, compensate_strategy,
                                     default_timeout_ms, default_max_attempts, default_backoff_ms,
                                     status, published_at,
                                     creator, create_time, updater, update_time, deleted)
VALUES
(5001, 'water_quality_full_test', 1, '水质综合检测流程（完整版）',
 '包含：采样→并行(PH+浊度)→汇算→分支(A/B/复检)→人工审核→归档→通知',
 'FAIL_FAST', 'NONE',
 600000, 3, 5000,
 'ACTIVE', NOW(),
 'admin', NOW(), 'admin', NOW(), 0);


-- ────────────────────────────────────────────────────────────────────────────
-- 6. 流程步骤编排 (pd_pipeline_step)
-- ────────────────────────────────────────────────────────────────────────────
--
-- DAG 结构图：
--
--   [s_register]
--       │
--   ┌───┴───┐         ← 并行（两个节点都依赖 s_register）
--   │       │
-- [s_ph]  [s_turbidity]
--   │       │
--   └───┬───┘
--       │
--   [s_calc]           ← 汇合（依赖 s_ph 和 s_turbidity）
--       │
--   [s_branch]         ← 条件分支
--       │
--   ┌───┼───────┐
--   │   │       │
--   │ [s_review]│      ← GRADE_B: 人工审核
--   │   │  [s_retest]  ← default: 复检任务
--   │   │       │
--   │ [s_archive_b]    ← 审核后归档
--   │           │
-- [s_archive_a] [s_archive_c]
--   │   │       │
--   └───┼───────┘
--       │
--   [s_notify]         ← 通知（汇合所有归档分支）
--

INSERT INTO pd_pipeline_step (id, pipeline_key, pipeline_version, node_id,
                               dispatch_mode, task_type, step_key, step_version,
                               depends_on, condition_expr, true_branch, false_branch, branches,
                               params_override, input_mapping, output_mapping,
                               timeout_ms, max_attempts, backoff_ms,
                               on_failure, compensate_node_id, compensate_step_key,
                               compensate_params, compensate_on,
                               command, runnable_standalone, mock_output,
                               sort_order, ui_position,
                               resource_enabled, zone_code, resource_wait_timeout_ms,
                               creator, create_time, updater, update_time, deleted)
VALUES

-- ① 采样登记 — 入口节点（无依赖）
(6001, 'water_quality_full_test', 1, 's_register',
 'DIRECT', NULL, 'sample_register', 1,
 NULL, NULL, NULL, NULL, NULL,
 '{"source":"incoming"}',
 NULL,
 NULL,
 10000, 1, NULL,
 'FAIL_PIPELINE', NULL, NULL, NULL, NULL,
 NULL, 1, NULL,
 1, '{"x":400,"y":50}',
 0, 'ZONE-A', NULL,
 'admin', NOW(), 'admin', NOW(), 0),

-- ② PH检测 — 仪器节点（依赖 s_register，与浊度并行）
(6002, 'water_quality_full_test', 1, 's_ph',
 'DIRECT', NULL, 'ph_measure', 1,
 '["s_register"]', NULL, NULL, NULL, NULL,
 NULL,
 '{"sampleId":"${s_register.sampleId}"}',
 '{"ph":"$.ph","temperature":"$.temperature"}',
 30000, 2, 5000,
 'RETRY_ONLY', NULL, NULL, NULL, NULL,
 'MEASURE', 1, NULL,
 2, '{"x":200,"y":200}',
 1, 'ZONE-A', 30000,
 'admin', NOW(), 'admin', NOW(), 0),

-- ③ 浊度检测 — 仪器节点（依赖 s_register，与PH并行）
(6003, 'water_quality_full_test', 1, 's_turbidity',
 'DIRECT', NULL, 'turbidity_measure', 1,
 '["s_register"]', NULL, NULL, NULL, NULL,
 NULL,
 '{"sampleId":"${s_register.sampleId}"}',
 '{"turbidity":"$.turbidity"}',
 60000, 2, 5000,
 'RETRY_ONLY', NULL, NULL, NULL, NULL,
 'MEASURE', 1, NULL,
 3, '{"x":600,"y":200}',
 1, 'ZONE-A', 30000,
 'admin', NOW(), 'admin', NOW(), 0),

-- ④ 数据汇算 — 计算节点（汇合并行分支，依赖 s_ph + s_turbidity）
(6004, 'water_quality_full_test', 1, 's_calc',
 'DIRECT', NULL, 'score_calc', 1,
 '["s_ph","s_turbidity"]', NULL, NULL, NULL, NULL,
 NULL,
 '{"phValue":"${s_ph.ph}","temperature":"${s_ph.temperature}","turbidity":"${s_turbidity.turbidity}"}',
 NULL,
 10000, 1, NULL,
 'FAIL_PIPELINE', NULL, NULL, NULL, NULL,
 NULL, 0, NULL,
 4, '{"x":400,"y":350}',
 0, NULL, NULL,
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑤ 条件分支 — 根据 grade 字段做 N 叉路由
(6005, 'water_quality_full_test', 1, 's_branch',
 'DIRECT', NULL, 'quality_branch', 1,
 '["s_calc"]',
 'grade',
 NULL, NULL,
 '{"GRADE_A":"s_archive_a","GRADE_B":"s_review","default":"s_retest"}',
 NULL,
 '{"score":"${s_calc.score}","grade":"${s_calc.grade}","qualified":"${s_calc.qualified}"}',
 NULL,
 5000, 1, NULL,
 'FAIL_PIPELINE', NULL, NULL, NULL, NULL,
 NULL, 0, NULL,
 5, '{"x":400,"y":480}',
 0, NULL, NULL,
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑥-A 直接归档（GRADE_A 优质水质）
(6006, 'water_quality_full_test', 1, 's_archive_a',
 'DIRECT', NULL, 'archive', 1,
 '["s_branch"]', NULL, NULL, NULL, NULL,
 '{"archiveType":"GRADE_A"}',
 '{"sampleId":"${s_register.sampleId}","score":"${s_calc.score}","grade":"${s_calc.grade}","ph":"${s_ph.ph}","turbidity":"${s_turbidity.turbidity}"}',
 NULL,
 10000, 1, NULL,
 'FAIL_PIPELINE', NULL, NULL, NULL, NULL,
 NULL, 0, NULL,
 6, '{"x":150,"y":620}',
 0, NULL, NULL,
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑦ 人工审核（GRADE_B 中等水质 → 需要人工确认）
(6007, 'water_quality_full_test', 1, 's_review',
 'DIRECT', NULL, 'manual_review', 1,
 '["s_branch"]', NULL, NULL, NULL, NULL,
 '{"waitReason":"水质评分中等(GRADE_B)，需人工确认是否放行","assignee":"quality_inspector"}',
 '{"sampleId":"${s_register.sampleId}","score":"${s_calc.score}","ph":"${s_ph.ph}","turbidity":"${s_turbidity.turbidity}"}',
 NULL,
 86400000, 1, NULL,
 'FAIL_PIPELINE', NULL, NULL, NULL, NULL,
 NULL, 0, NULL,
 7, '{"x":400,"y":620}',
 0, NULL, NULL,
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑥-B 审核后归档
(6008, 'water_quality_full_test', 1, 's_archive_b',
 'DIRECT', NULL, 'archive', 1,
 '["s_review"]', NULL, NULL, NULL, NULL,
 '{"archiveType":"GRADE_B_REVIEWED"}',
 '{"sampleId":"${s_register.sampleId}","score":"${s_calc.score}","grade":"${s_calc.grade}","reviewResult":"${s_review.approved}","reviewComment":"${s_review.reviewComment}"}',
 NULL,
 10000, 1, NULL,
 'FAIL_PIPELINE', NULL, NULL, NULL, NULL,
 NULL, 0, NULL,
 8, '{"x":400,"y":760}',
 0, NULL, NULL,
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑧ 复检任务（default 不合格 → 下发复检任务，走 QUEUED 模式）
(6009, 'water_quality_full_test', 1, 's_retest',
 'QUEUED', 'AGV_TRANSPORT', 'retest_task', 1,
 '["s_branch"]', NULL, NULL, NULL, NULL,
 '{"reason":"水质不合格，需复检"}',
 '{"sampleId":"${s_register.sampleId}","score":"${s_calc.score}","ph":"${s_ph.ph}","turbidity":"${s_turbidity.turbidity}"}',
 NULL,
 300000, 1, NULL,
 'FAIL_PIPELINE', NULL, NULL, NULL, NULL,
 NULL, 0, NULL,
 9, '{"x":650,"y":620}',
 0, 'ZONE-A', NULL,
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑥-C 复检后归档
(6010, 'water_quality_full_test', 1, 's_archive_c',
 'DIRECT', NULL, 'archive', 1,
 '["s_retest"]', NULL, NULL, NULL, NULL,
 '{"archiveType":"RETEST"}',
 '{"sampleId":"${s_register.sampleId}","score":"${s_calc.score}","retestResult":"${s_retest.taskId}"}',
 NULL,
 10000, 1, NULL,
 'FAIL_PIPELINE', NULL, NULL, NULL, NULL,
 NULL, 0, NULL,
 10, '{"x":650,"y":760}',
 0, NULL, NULL,
 'admin', NOW(), 'admin', NOW(), 0),

-- ⑨ 通知 — 汇合所有归档分支后发送通知
(6011, 'water_quality_full_test', 1, 's_notify',
 'DIRECT', NULL, 'result_notify', 1,
 '["s_archive_a","s_archive_b","s_archive_c"]', NULL, NULL, NULL, NULL,
 '{"channel":"DING_TALK","templateCode":"water_quality_result"}',
 '{"sampleId":"${s_register.sampleId}","grade":"${s_calc.grade}","score":"${s_calc.score}"}',
 NULL,
 10000, 1, NULL,
 'SKIP', NULL, NULL, NULL, NULL,
 NULL, 0, NULL,
 11, '{"x":400,"y":900}',
 0, NULL, NULL,
 'admin', NOW(), 'admin', NOW(), 0);


-- ────────────────────────────────────────────────────────────────────────────
-- 7. 验证查询（执行后检查数据是否正确插入）
-- ────────────────────────────────────────────────────────────────────────────

-- 查看设备配置
-- SELECT device_id, device_name, device_type, protocol, callback_mode, status, enabled
-- FROM lab_device_info WHERE deleted = 0 ORDER BY id;

-- 查看指令配置
-- SELECT device_type, command_code, http_method, http_path, codec_id, poll_path
-- FROM lab_device_command WHERE deleted = 0 ORDER BY id;

-- 查看解析规则
-- SELECT id, codec_name, parse_type, field_mapping, valid_range
-- FROM lab_device_codec WHERE deleted = 0;

-- 查看步骤定义
-- SELECT step_key, step_type, device_type, command, executor, bean_name
-- FROM pd_step_definition WHERE deleted = 0 ORDER BY id;

-- 查看流程编排 DAG
-- SELECT node_id, step_key, dispatch_mode, depends_on, condition_expr, branches
-- FROM pd_pipeline_step
-- WHERE pipeline_key = 'water_quality_full_test' AND pipeline_version = 1 AND deleted = 0
-- ORDER BY sort_order;
