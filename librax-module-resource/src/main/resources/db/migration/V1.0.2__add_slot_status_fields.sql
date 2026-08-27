-- 库位表新增占用状态字段
ALTER TABLE `lab_slot_info`
    ADD COLUMN `status`        VARCHAR(16)  NOT NULL DEFAULT 'EMPTY' COMMENT '占用状态：EMPTY=空闲 OCCUPIED=已占用 DISABLED=停用' AFTER `capacity`,
    ADD COLUMN `current_count` INT          NOT NULL DEFAULT 0       COMMENT '当前已放入的容器数量，达到 capacity 时 status 自动置为 OCCUPIED' AFTER `status`,
    ADD COLUMN `occupied_by`   VARCHAR(64)           DEFAULT NULL    COMMENT '当前占用的物料实例ID（lab_material_instance.instance_id），空闲时为NULL' AFTER `current_count`,
    ADD KEY `idx_zone_status` (`zone_code`, `status`, `deleted`);
