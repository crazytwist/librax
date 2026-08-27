-- 物料实例表：新增样本实验参数字段
-- 用于存储每个样本独立的实验配置，如 {"targetPh": 7.2, "volumeUl": 500, "temperature": 37}
ALTER TABLE `lab_material_instance`
    ADD COLUMN `params` json DEFAULT NULL
        COMMENT '样本实验参数，JSON 格式，如 {"targetPh": 7.2, "volumeUl": 500}，步骤执行时从此字段读取'
    AFTER `current_count`;
