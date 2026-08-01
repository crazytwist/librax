-- 让 AGV 车次装载计划支持两种"物料就绪"方式：
-- WAREHOUSE（默认，补料现状）：逐件下发 prepareMaterials，等仓储 slot/ready 回调；
-- DIRECT（机台间转运新增）：物料已经在源库位上，跳过仓储环节直接标记就绪。
-- 由 AgvLoadPlanServiceImpl.startWarehouseOrchestration() 的 inputParams.sourceReadyMode 决定，
-- 持久化到本列，保证多波次续跑时沿用同一种模式。

ALTER TABLE lab_agv_load_plan
    ADD COLUMN source_ready_mode VARCHAR(16) NOT NULL DEFAULT 'WAREHOUSE'
        COMMENT '每波物料就绪方式：WAREHOUSE-等仓储回调，DIRECT-源库位已就绪直接跳过' AFTER warehouse_callback_url;
