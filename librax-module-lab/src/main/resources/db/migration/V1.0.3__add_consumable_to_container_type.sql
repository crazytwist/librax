-- 容器类型表：新增耗材属性字段（对应 ContainerTypeDO.consumable）
-- 耗材属性：0-非耗材（载具/周转容器），1-耗材（吸头、滤芯等一次性消耗，出库后需补充）
ALTER TABLE lab_container_type
    ADD COLUMN consumable BIT(1) NOT NULL DEFAULT b'0'
        COMMENT '耗材属性：0-非耗材 1-耗材';
