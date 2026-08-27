-- ================================================================
-- 载具柜货架 + 库位种子数据（依据现场布局图，以表格标注为准）
--
-- 物理结构：一台载具柜，左右两半各 6层 × 9列
--   RACK-L（左侧）：54 库位
--   RACK-R（右侧）：54 库位（第6层 C1-4=50UL TIP，C5-9=300UL TIP）
--   合计 108 个库位
--
-- 库位编码：{rack_id}-R{row}-C{col}（单层货架，省略层号 L1）
-- position_layer 固定填 1，对应 lab_rack_info.layer_count=1
-- 坐标间距：列 120mm / 行 150mm（请按实测调整）
-- 区域：ZONE-A
-- ================================================================

INSERT INTO lab_rack_info
    (rack_id, rack_name, rack_type, zone_code,
     coord_x, coord_y, coord_z,
     row_count, col_count, layer_count,
     enabled, remark, creator, updater, create_time, update_time, deleted)
VALUES
('RACK-L', '载具柜-左侧', 'RACK', 'ZONE-A', 1000, 2000, 800, 6, 9, 1, 1, '色谱瓶/样品瓶/废液瓶', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R', '载具柜-右侧', 'RACK', 'ZONE-A', 2080, 2000, 800, 6, 9, 1, 1, '加样头/TIP头/滤芯/外壳', 'admin', 'admin', NOW(3), NOW(3), b'0');

-- 共 108 个库位
INSERT INTO lab_slot_info
    (slot_id, slot_name, slot_type, slot_usage, zone_code, rack_id,
     position_row, position_col, position_layer,
     coord_x, coord_y, coord_z,
     owner_id, local_index,
     capacity, status, current_count, occupied_by,
     enabled, remark, creator, updater, create_time, update_time, deleted)
VALUES
-- ======== RACK-L ========
-- 色谱瓶
('RACK-L-R1-C1', '载具柜-左侧 色谱瓶 R1C1', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 1, 1, 1000, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R1-C2', '载具柜-左侧 色谱瓶 R1C2', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 2, 1, 1120, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R1-C3', '载具柜-左侧 色谱瓶 R1C3', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 3, 1, 1240, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R1-C4', '载具柜-左侧 色谱瓶 R1C4', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 4, 1, 1360, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R1-C5', '载具柜-左侧 色谱瓶 R1C5', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 5, 1, 1480, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R1-C6', '载具柜-左侧 色谱瓶 R1C6', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 6, 1, 1600, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R1-C7', '载具柜-左侧 色谱瓶 R1C7', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 7, 1, 1720, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R1-C8', '载具柜-左侧 色谱瓶 R1C8', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 8, 1, 1840, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R1-C9', '载具柜-左侧 色谱瓶 R1C9', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 1, 9, 1, 1960, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C1', '载具柜-左侧 色谱瓶 R2C1', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 1, 1, 1000, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C2', '载具柜-左侧 色谱瓶 R2C2', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 2, 1, 1120, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C3', '载具柜-左侧 色谱瓶 R2C3', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 3, 1, 1240, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C4', '载具柜-左侧 色谱瓶 R2C4', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 4, 1, 1360, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C5', '载具柜-左侧 色谱瓶 R2C5', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 5, 1, 1480, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C6', '载具柜-左侧 色谱瓶 R2C6', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 6, 1, 1600, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C7', '载具柜-左侧 色谱瓶 R2C7', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 7, 1, 1720, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C8', '载具柜-左侧 色谱瓶 R2C8', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 8, 1, 1840, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R2-C9', '载具柜-左侧 色谱瓶 R2C9', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-L', 2, 9, 1, 1960, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_COL_CHROMA', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 125ML样品瓶
('RACK-L-R3-C1', '载具柜-左侧 125ML样品瓶 R3C1', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 1, 1, 1000, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R3-C2', '载具柜-左侧 125ML样品瓶 R3C2', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 2, 1, 1120, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R3-C3', '载具柜-左侧 125ML样品瓶 R3C3', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 3, 1, 1240, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R3-C4', '载具柜-左侧 125ML样品瓶 R3C4', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 4, 1, 1360, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R3-C5', '载具柜-左侧 125ML样品瓶 R3C5', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 5, 1, 1480, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R3-C6', '载具柜-左侧 125ML样品瓶 R3C6', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 6, 1, 1600, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R3-C7', '载具柜-左侧 125ML样品瓶 R3C7', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 7, 1, 1720, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R3-C8', '载具柜-左侧 125ML样品瓶 R3C8', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 8, 1, 1840, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R3-C9', '载具柜-左侧 125ML样品瓶 R3C9', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 3, 9, 1, 1960, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 16ML样品瓶
('RACK-L-R4-C1', '载具柜-左侧 16ML样品瓶 R4C1', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 1, 1, 1000, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R4-C2', '载具柜-左侧 16ML样品瓶 R4C2', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 2, 1, 1120, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R4-C3', '载具柜-左侧 16ML样品瓶 R4C3', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 3, 1, 1240, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R4-C4', '载具柜-左侧 16ML样品瓶 R4C4', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 4, 1, 1360, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R4-C5', '载具柜-左侧 16ML样品瓶 R4C5', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 5, 1, 1480, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R4-C6', '载具柜-左侧 16ML样品瓶 R4C6', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 6, 1, 1600, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R4-C7', '载具柜-左侧 16ML样品瓶 R4C7', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 7, 1, 1720, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R4-C8', '载具柜-左侧 16ML样品瓶 R4C8', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 8, 1, 1840, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R4-C9', '载具柜-左侧 16ML样品瓶 R4C9', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 4, 9, 1, 1960, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 40ML样品瓶
('RACK-L-R5-C1', '载具柜-左侧 40ML样品瓶 R5C1', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 1, 1, 1000, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R5-C2', '载具柜-左侧 40ML样品瓶 R5C2', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 2, 1, 1120, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R5-C3', '载具柜-左侧 40ML样品瓶 R5C3', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 3, 1, 1240, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R5-C4', '载具柜-左侧 40ML样品瓶 R5C4', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 4, 1, 1360, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R5-C5', '载具柜-左侧 40ML样品瓶 R5C5', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 5, 1, 1480, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R5-C6', '载具柜-左侧 40ML样品瓶 R5C6', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 6, 1, 1600, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R5-C7', '载具柜-左侧 40ML样品瓶 R5C7', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 7, 1, 1720, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R5-C8', '载具柜-左侧 40ML样品瓶 R5C8', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 8, 1, 1840, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R5-C9', '载具柜-左侧 40ML样品瓶 R5C9', 'FIXED', 'SAMPLE', 'ZONE-A', 'RACK-L', 5, 9, 1, 1960, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 废液瓶
('RACK-L-R6-C1', '载具柜-左侧 废液瓶 R6C1', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 1, 1, 1000, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R6-C2', '载具柜-左侧 废液瓶 R6C2', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 2, 1, 1120, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R6-C3', '载具柜-左侧 废液瓶 R6C3', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 3, 1, 1240, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R6-C4', '载具柜-左侧 废液瓶 R6C4', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 4, 1, 1360, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R6-C5', '载具柜-左侧 废液瓶 R6C5', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 5, 1, 1480, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R6-C6', '载具柜-左侧 废液瓶 R6C6', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 6, 1, 1600, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R6-C7', '载具柜-左侧 废液瓶 R6C7', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 7, 1, 1720, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R6-C8', '载具柜-左侧 废液瓶 R6C8', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 8, 1, 1840, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-L-R6-C9', '载具柜-左侧 废液瓶 R6C9', 'FIXED', 'WASTE', 'ZONE-A', 'RACK-L', 6, 9, 1, 1960, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_40ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- ======== RACK-R ========
-- 125ML加样头
('RACK-R-R1-C1', '载具柜-右侧 125ML加样头 R1C1', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 1, 1, 2080, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R1-C2', '载具柜-右侧 125ML加样头 R1C2', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 2, 1, 2200, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R1-C3', '载具柜-右侧 125ML加样头 R1C3', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 3, 1, 2320, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R1-C4', '载具柜-右侧 125ML加样头 R1C4', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 4, 1, 2440, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R1-C5', '载具柜-右侧 125ML加样头 R1C5', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 5, 1, 2560, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R1-C6', '载具柜-右侧 125ML加样头 R1C6', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 6, 1, 2680, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R1-C7', '载具柜-右侧 125ML加样头 R1C7', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 7, 1, 2800, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R1-C8', '载具柜-右侧 125ML加样头 R1C8', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 8, 1, 2920, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R1-C9', '载具柜-右侧 125ML加样头 R1C9', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 1, 9, 1, 3040, 2000, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_125ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 16ML加样头
('RACK-R-R2-C1', '载具柜-右侧 16ML加样头 R2C1', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 1, 1, 2080, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R2-C2', '载具柜-右侧 16ML加样头 R2C2', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 2, 1, 2200, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R2-C3', '载具柜-右侧 16ML加样头 R2C3', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 3, 1, 2320, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R2-C4', '载具柜-右侧 16ML加样头 R2C4', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 4, 1, 2440, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R2-C5', '载具柜-右侧 16ML加样头 R2C5', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 5, 1, 2560, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R2-C6', '载具柜-右侧 16ML加样头 R2C6', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 6, 1, 2680, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R2-C7', '载具柜-右侧 16ML加样头 R2C7', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 7, 1, 2800, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R2-C8', '载具柜-右侧 16ML加样头 R2C8', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 8, 1, 2920, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R2-C9', '载具柜-右侧 16ML加样头 R2C9', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 2, 9, 1, 3040, 2150, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TUBE_16ML', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 1000UL TIP头
('RACK-R-R3-C1', '载具柜-右侧 1000UL TIP头 R3C1', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 1, 1, 2080, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R3-C2', '载具柜-右侧 1000UL TIP头 R3C2', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 2, 1, 2200, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R3-C3', '载具柜-右侧 1000UL TIP头 R3C3', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 3, 1, 2320, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R3-C4', '载具柜-右侧 1000UL TIP头 R3C4', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 4, 1, 2440, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R3-C5', '载具柜-右侧 1000UL TIP头 R3C5', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 5, 1, 2560, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R3-C6', '载具柜-右侧 1000UL TIP头 R3C6', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 6, 1, 2680, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R3-C7', '载具柜-右侧 1000UL TIP头 R3C7', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 7, 1, 2800, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R3-C8', '载具柜-右侧 1000UL TIP头 R3C8', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 8, 1, 2920, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R3-C9', '载具柜-右侧 1000UL TIP头 R3C9', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 3, 9, 1, 3040, 2300, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_1000UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 滤芯
('RACK-R-R4-C1', '载具柜-右侧 滤芯 R4C1', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 1, 1, 2080, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R4-C2', '载具柜-右侧 滤芯 R4C2', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 2, 1, 2200, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R4-C3', '载具柜-右侧 滤芯 R4C3', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 3, 1, 2320, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R4-C4', '载具柜-右侧 滤芯 R4C4', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 4, 1, 2440, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R4-C5', '载具柜-右侧 滤芯 R4C5', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 5, 1, 2560, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R4-C6', '载具柜-右侧 滤芯 R4C6', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 6, 1, 2680, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R4-C7', '载具柜-右侧 滤芯 R4C7', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 7, 1, 2800, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R4-C8', '载具柜-右侧 滤芯 R4C8', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 8, 1, 2920, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R4-C9', '载具柜-右侧 滤芯 R4C9', 'FIXED', 'REAGENT', 'ZONE-A', 'RACK-R', 4, 9, 1, 3040, 2450, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_FILTER_CART', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 外壳
('RACK-R-R5-C1', '载具柜-右侧 外壳 R5C1', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 1, 1, 2080, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R5-C2', '载具柜-右侧 外壳 R5C2', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 2, 1, 2200, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R5-C3', '载具柜-右侧 外壳 R5C3', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 3, 1, 2320, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R5-C4', '载具柜-右侧 外壳 R5C4', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 4, 1, 2440, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R5-C5', '载具柜-右侧 外壳 R5C5', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 5, 1, 2560, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R5-C6', '载具柜-右侧 外壳 R5C6', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 6, 1, 2680, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R5-C7', '载具柜-右侧 外壳 R5C7', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 7, 1, 2800, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R5-C8', '载具柜-右侧 外壳 R5C8', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 8, 1, 2920, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R5-C9', '载具柜-右侧 外壳 R5C9', 'FIXED', 'ANY', 'ZONE-A', 'RACK-R', 5, 9, 1, 3040, 2600, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_BOTTLE_OUTER', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 50UL TIP头
('RACK-R-R6-C1', '载具柜-右侧 50UL TIP头 R6C1', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 1, 1, 2080, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_50UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R6-C2', '载具柜-右侧 50UL TIP头 R6C2', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 2, 1, 2200, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_50UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R6-C3', '载具柜-右侧 50UL TIP头 R6C3', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 3, 1, 2320, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_50UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R6-C4', '载具柜-右侧 50UL TIP头 R6C4', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 4, 1, 2440, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_50UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
-- 300UL TIP头钢针
('RACK-R-R6-C5', '载具柜-右侧 300UL TIP头钢针 R6C5', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 5, 1, 2560, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_300UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R6-C6', '载具柜-右侧 300UL TIP头钢针 R6C6', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 6, 1, 2680, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_300UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R6-C7', '载具柜-右侧 300UL TIP头钢针 R6C7', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 7, 1, 2800, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_300UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R6-C8', '载具柜-右侧 300UL TIP头钢针 R6C8', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 8, 1, 2920, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_300UL', 'admin', 'admin', NOW(3), NOW(3), b'0'),
('RACK-R-R6-C9', '载具柜-右侧 300UL TIP头钢针 R6C9', 'FIXED', 'TIP', 'ZONE-A', 'RACK-R', 6, 9, 1, 3040, 2750, 800, NULL, NULL, 1, 'EMPTY', 0, NULL, 1, '载具类型:CARRIER_TIP_300UL', 'admin', 'admin', NOW(3), NOW(3), b'0');
