-- 在流程执行表上增加执行主体ID
-- subject_id 存储本次执行关联的业务对象ID，目前用于样本（lab_sample_info.sample_id）
-- 通用设计：未来可关联工单、批次等其他实体，flow 模块不感知具体类型
ALTER TABLE pe_pipeline_execution
    ADD COLUMN IF NOT EXISTS subject_id VARCHAR(64) DEFAULT NULL
        COMMENT '执行主体业务ID，如样本ID（lab_sample_info.sample_id）。由启动方通过 inputParams[sampleId] 传入，引擎自动提取存储，用于 execution→sample 反向查询';

-- 建索引，支持「查某个样本的所有执行」
CREATE INDEX IF NOT EXISTS idx_pe_execution_subject_id
    ON pe_pipeline_execution (subject_id);
