-- 从 lab_material_instance 移除 params 字段（归属错误，实验参数应挂载在样本上）
ALTER TABLE lab_material_instance DROP COLUMN IF EXISTS params;

-- 在 lab_sample_info 新增 experiment_params 字段，用于存储每个样本的实验参数
ALTER TABLE lab_sample_info
    ADD COLUMN IF NOT EXISTS experiment_params json DEFAULT NULL COMMENT '实验参数，JSON 格式，如 {"targetPh": 7.2, "volumeUl": 500}，流程步骤执行时从此字段读取';
