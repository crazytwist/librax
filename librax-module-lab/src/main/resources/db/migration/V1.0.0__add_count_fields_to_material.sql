-- 物料实例表：新增固体/耗材数量字段
-- current_vol_ul 已有（液体），对称补充 current_count（固体）
ALTER TABLE `lab_material_instance`
    ADD COLUMN `current_count` int DEFAULT NULL
        COMMENT '当前数量（个），固体/耗材类填写，液体类为NULL，步骤消耗后更新'
    AFTER `current_vol_ul`;

-- 物料消耗记录表：新增数量变化字段，与体积字段对称
-- 液体用 vol_before_ul / vol_change_ul / vol_after_ul
-- 固体用 count_before / count_change / count_after
ALTER TABLE `lab_material_consumption`
    ADD COLUMN `count_before`  int DEFAULT NULL COMMENT '操作前数量（个），固体/耗材类填写' AFTER `vol_after_ul`,
    ADD COLUMN `count_change`  int DEFAULT NULL COMMENT '数量变化量（个），消耗为负值如-2，产生为正值如+5' AFTER `count_before`,
    ADD COLUMN `count_after`   int DEFAULT NULL COMMENT '操作后数量（个），= count_before + count_change' AFTER `count_change`;
