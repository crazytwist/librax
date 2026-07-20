-- 流程定义表增加默认入参字段
-- 启动流程时若调用方未传参则使用此字段，传参则 merge（调用方传参优先）
ALTER TABLE pd_pipeline_definition
    ADD COLUMN default_input_params TEXT NULL COMMENT '流程默认入参（JSON 对象），启动时未传参则使用，传参则合并（传参优先）'
        AFTER default_backoff_ms;
