-- ============================================================================
-- 下料流程（agv_warehouse_return_flow）测试数据
-- 场景：1件 SAMPLE_TUBE 在 HCL 站位库位 HCL-SLOT-01，调用下料接口送回仓储
--
-- 依赖的配置默认值（ReturnProperties）：
--   agvSlotIds       : [R-A-1 ... R-A-8]
--   transitSlotIds   : [R-E-1, R-E-2]
--   warehouseReturnZone: WAREHOUSE-ZONE-A
--   agvDeviceId      : AGV-01
--   pipelineKey      : agv_warehouse_return_flow
--
-- 执行顺序：本文件 → 启动服务 → 调用接口（见文末注释）
-- 清理：文末 CLEANUP 部分可重复执行测试
-- ============================================================================

-- ① 站位库位（HCL 后处理站，物料当前在此）
-- area = rack_id = HCL-RACK, pos = local_index = '1'
INSERT IGNORE INTO lab_slot_info
    (slot_id, slot_name, slot_type, slot_usage, zone_code, rack_id,
     position_row, position_col, position_layer, local_index,
     owner_id, capacity, status, current_count, occupied_by,
     enabled, remark, creator, updater, create_time, update_time, deleted)
VALUES
('HCL-SLOT-01', 'HCL后处理站位-01', 'FIXED', NULL, 'HCL-ZONE', 'HCL-RACK',
 1, 1, 1, '1',
 NULL, 1, 'OCCUPIED', 1, 'TEST-MAT-001',
 1, '下料测试-HCL站位', 'admin', 'admin', NOW(3), NOW(3), b'0');

-- ② AGV 载台槽位（归属 AGV-01，local_index 对应 AGV 孔位编号）
INSERT IGNORE INTO lab_slot_info
    (slot_id, slot_name, slot_type, slot_usage, zone_code, rack_id,
     local_index, owner_id,
     capacity, status, current_count, occupied_by,
     enabled, remark, creator, updater, create_time, update_time, deleted)
VALUES
('R-A-1', 'AGV-01 槽位1', 'AGV', NULL, NULL, NULL, '1', 'AGV-01', 1, 'EMPTY', 0, NULL, 1, 'AGV载台槽', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('R-A-2', 'AGV-01 槽位2', 'AGV', NULL, NULL, NULL, '2', 'AGV-01', 1, 'EMPTY', 0, NULL, 1, 'AGV载台槽', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('R-A-3', 'AGV-01 槽位3', 'AGV', NULL, NULL, NULL, '3', 'AGV-01', 1, 'EMPTY', 0, NULL, 1, 'AGV载台槽', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('R-A-4', 'AGV-01 槽位4', 'AGV', NULL, NULL, NULL, '4', 'AGV-01', 1, 'EMPTY', 0, NULL, 1, 'AGV载台槽', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('R-A-5', 'AGV-01 槽位5', 'AGV', NULL, NULL, NULL, '5', 'AGV-01', 1, 'EMPTY', 0, NULL, 1, 'AGV载台槽', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('R-A-6', 'AGV-01 槽位6', 'AGV', NULL, NULL, NULL, '6', 'AGV-01', 1, 'EMPTY', 0, NULL, 1, 'AGV载台槽', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('R-A-7', 'AGV-01 槽位7', 'AGV', NULL, NULL, NULL, '7', 'AGV-01', 1, 'EMPTY', 0, NULL, 1, 'AGV载台槽', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('R-A-8', 'AGV-01 槽位8', 'AGV', NULL, NULL, NULL, '8', 'AGV-01', 1, 'EMPTY', 0, NULL, 1, 'AGV载台槽', 'admin', 'admin', NOW(3), NOW(3), b'0');

-- ③ 仓储中转位（AGV 卸料到此，仓储机械臂从此取走）
-- slot_usage='TRANSIT' 防止被 reserveWarehouseSlots 误选为仓储目标库位（Bug2 fix）
-- area = rack_id = TRANSIT-RACK, pos = local_index = '1'/'2'（对应 AGV_CC 工作流孔位1~2）
INSERT IGNORE INTO lab_slot_info
    (slot_id, slot_name, slot_type, slot_usage, zone_code, rack_id,
     position_row, position_col, position_layer, local_index,
     owner_id, capacity, status, current_count, occupied_by,
     enabled, remark, creator, updater, create_time, update_time, deleted)
VALUES
('R-E-1', '仓储中转位-01', 'FIXED', 'TRANSIT', 'TRANSIT-ZONE', 'TRANSIT-RACK',
 1, 1, 1, '1', NULL, 1, 'EMPTY', 0, NULL, 1, '仓储中转位，slot_usage=TRANSIT防止被误选', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('R-E-2', '仓储中转位-02', 'FIXED', 'TRANSIT', 'TRANSIT-ZONE', 'TRANSIT-RACK',
 1, 2, 1, '2', NULL, 1, 'EMPTY', 0, NULL, 1, '仓储中转位，slot_usage=TRANSIT防止被误选', 'admin', 'admin', NOW(3), NOW(3), b'0');

-- ④ 仓储目标库位（机械臂把物料放到这里）
-- slot_usage='SAMPLE_TUBE' 与测试物料 typeCode 对齐
-- area = rack_id = WAREHOUSE-RACK-01, pos = local_index = '1'/'2'
INSERT IGNORE INTO lab_slot_info
    (slot_id, slot_name, slot_type, slot_usage, zone_code, rack_id,
     position_row, position_col, position_layer, local_index,
     owner_id, capacity, status, current_count, occupied_by,
     enabled, remark, creator, updater, create_time, update_time, deleted)
VALUES
('W-SLOT-01', '仓储库位-01', 'FIXED', 'SAMPLE_TUBE', 'WAREHOUSE-ZONE-A', 'WAREHOUSE-RACK-01',
 1, 1, 1, '1', NULL, 1, 'EMPTY', 0, NULL, 1, '仓储存储库位', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('W-SLOT-02', '仓储库位-02', 'FIXED', 'SAMPLE_TUBE', 'WAREHOUSE-ZONE-A', 'WAREHOUSE-RACK-01',
 1, 2, 1, '2', NULL, 1, 'EMPTY', 0, NULL, 1, '仓储存储库位', 'admin', 'admin', NOW(3), NOW(3), b'0');

-- ⑤ 测试物料实例（当前在 HCL-SLOT-01）
INSERT IGNORE INTO lab_material_instance
    (instance_id, type_code, barcode, slot_id, zone_code,
     content_type, material_code, status,
     creator, updater, create_time, update_time, deleted)
VALUES
('TEST-MAT-001', 'SAMPLE_TUBE', 'BARCODE-TEST-001', 'HCL-SLOT-01', 'HCL-ZONE',
 'SAMPLE', 'SAMPLE_TUBE', 'AVAILABLE',
 'admin', 'admin', NOW(3), NOW(3), b'0');

-- ⑥ 确认路由配置（HCL_TO_WAREHOUSE 应已由 V1.0.7 migration 创建）
-- 如不存在则手动插入：
INSERT IGNORE INTO lab_agv_route_config
    (route_code, route_name, source_station_code, dest_station_code,
     task_type, plate_type,
     step1_task_name, step2_task_name, step2_station_name, step3_task_name,
     enabled, remark, creator, updater, create_time, update_time, deleted)
VALUES
('HCL_TO_WAREHOUSE', '后处理工站 → 仓储（下料）', 'HCL', 'WAREHOUSE', '1', '1',
 'HCL_AGV', 'AgvMove', 'WAREHOUSE', 'AGV_CC',
 1, '下料测试路由', 'admin', 'admin', NOW(3), NOW(3), 0);


-- ============================================================================
-- 调用下料接口（服务启动后执行）：
--
-- POST http://localhost:48080/app-api/biz/material/return
-- Content-Type: application/json
-- Authorization: Bearer <token>
--
-- {
--   "requestId": "RET-TEST-001",
--   "routeCode": "HCL_TO_WAREHOUSE",
--   "sourceSlotIds": ["HCL-SLOT-01"],
--   "requesterDeviceId": "POST-PROCESSOR-MOCK-01",
--   "notifyOnComplete": true,
--   "notifyDeviceType": "POST_PROCESSOR",
--   "notifyCommand": "unloadComplete"
-- }
--
-- 期望响应：
-- {
--   "code": 0,
--   "data": {
--     "requestId": "RET-TEST-001",
--     "executionId": "<uuid>",
--     "count": 1,
--     "items": [{
--       "instanceId": "TEST-MAT-001",
--       "sourceSlotId": "HCL-SLOT-01",
--       "agvSlotId": "R-A-1",
--       "transitSlotId": "R-E-1",
--       "warehouseTargetLocation": "W-SLOT-01"   ← 具体库位，不是区域
--     }]
--   }
-- }
--
-- 验证 DB：
--   SELECT slot_id, status FROM lab_slot_info
--   WHERE slot_id IN ('W-SLOT-01','W-SLOT-02','R-E-1','R-E-2');
--   → W-SLOT-01 应为 RESERVED（已预占）
-- ============================================================================


-- ============================================================================
-- CLEANUP（重新测试前执行）
-- ============================================================================
-- DELETE FROM lab_material_instance WHERE instance_id = 'TEST-MAT-001';
-- DELETE FROM lab_slot_info WHERE slot_id IN (
--     'HCL-SLOT-01','R-A-1','R-A-2','R-A-3','R-A-4',
--     'R-A-5','R-A-6','R-A-7','R-A-8',
--     'R-E-1','R-E-2','W-SLOT-01','W-SLOT-02');
-- DELETE FROM lab_agv_route_config WHERE route_code = 'HCL_TO_WAREHOUSE';
