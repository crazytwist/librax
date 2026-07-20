-- 统一仓储命令：prepareMaterials 兼容补料与下料，删除独立的 returnMaterials
-- 统一回调接口：/resource/slot/warehouse-callback

-- ① prepareMaterials 模版加入 requestId（幂等键）和 materialId（物料实例）
-- callbackUrl 由 injectSysFields 从 params 自动注入 body，无需在 template 中声明
UPDATE lab_device_command
SET request_template = '{"requestId":"${requestId}","transferTaskId":"${transferTaskId}","materialId":"${materialId}","containerType":"${containerType}","fromLocation":"${fromLocation}","toLocation":"${toLocation}"}'
WHERE device_type = 'WAREHOUSE' AND command_code = 'prepareMaterials';

-- ② 删除独立的 returnMaterials（已统一到 prepareMaterials）
DELETE FROM lab_device_command WHERE device_type = 'WAREHOUSE' AND command_code = 'returnMaterials';

-- ③ 更新补料流程默认回调 URL
UPDATE pd_pipeline_definition
SET default_input_params = JSON_SET(default_input_params, '$.warehouseCallbackUrl',
    'http://localhost:48080/app-api/resource/slot/warehouse-callback')
WHERE pipeline_key = 'agv_warehouse_transfer_flow';

-- ④ 更新下料流程默认回调 URL
UPDATE pd_pipeline_definition
SET default_input_params = JSON_SET(default_input_params, '$.warehouseCallbackUrl',
    'http://localhost:48080/app-api/resource/slot/warehouse-callback')
WHERE pipeline_key = 'agv_warehouse_return_flow';
