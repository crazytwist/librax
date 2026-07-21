-- lab_agv_load_item 补充 container_type 字段
-- DO 中已有该字段，建表时遗漏，补丁修复。

ALTER TABLE lab_agv_load_item
    ADD COLUMN container_type VARCHAR(64) NULL COMMENT '容器类型编码，对应 lab_container_type.type_code' AFTER instance_id;
