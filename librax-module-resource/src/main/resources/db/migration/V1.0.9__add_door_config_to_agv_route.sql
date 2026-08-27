-- 给 AGV 路由配置加"是否需要开门"开关。
-- 不同路由的目标站点是否有门禁不一样：有的补料要开门，有的不要，
-- 由这三列决定，运行时透传给 door_gate 流程节点（doorGateBean）。

-- 注：MySQL 8.0.29 以前的版本不支持 ADD COLUMN IF NOT EXISTS，这里不加该子句；
-- 这三列是首次新增，直接执行即可，重复执行会报"列已存在"，属预期行为。
ALTER TABLE lab_agv_route_config
    ADD COLUMN door_required     TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '目标站点前是否需要开关门：1-需要，0-不需要' AFTER enabled,
    ADD COLUMN door_device_id    VARCHAR(64) NULL COMMENT '门禁设备类型（lab_device_command.device_type），door_required=1时生效，留空默认 Feeding-Reaction' AFTER door_required,
    ADD COLUMN door_command_code VARCHAR(64) NULL COMMENT '开关门指令码（lab_device_command.command_code），door_required=1时生效，留空默认 OpenCloseDoor' AFTER door_device_id;
