-- 为 pd_pipeline_step 表添加 step_type 字段
-- 允许 pipeline_step 级别覆盖 step_definition 中的步骤类型
-- 支持的值：INSTRUMENT | COMPUTE | CONDITION | WAIT | NOTIFY | SAMPLE_SPLIT | MANUAL | UNIT_LAUNCHER

ALTER TABLE pd_pipeline_step ADD COLUMN IF NOT EXISTS step_type VARCHAR(31) DEFAULT NULL;

COMMENT ON COLUMN pd_pipeline_step.step_type IS '步骤类型，覆盖 pd_step_definition.step_type。非空时引擎优先使用此值';
