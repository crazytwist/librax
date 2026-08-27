-- AGV 多轮装载表。内容与 resource 模块 V1.0.3 Flyway migration 保持一致。
SOURCE librax-module-resource/src/main/resources/db/migration/V1.0.3__add_agv_multi_load_plan.sql;

-- 开启方式（放入启动流程 inputParams）：
-- "multiLoadEnabled": true,
-- "sourceWindowCapacity": 2,
-- "agvCapacity": 8,
-- "allowPartialLoad": false,
-- "loadStation": "AP1"
