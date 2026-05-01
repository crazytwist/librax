-- ================================================================
-- lab_resource_config 资源配置表
-- 一行 = 一个可被调度占用的资源(设备工位/AGV/操作台等)
-- 配置资源的归属关系,运行时锁放 Redis 不入表
-- ================================================================
CREATE TABLE `lab_resource_config` (
                                       `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `resource_id`    VARCHAR(64)  NOT NULL COMMENT '资源唯一ID,建议与设备ID一致,如 PH-METER-01、AGV-01',
                                       `resource_type`  VARCHAR(32)  NOT NULL COMMENT '资源类型,如 PH_METER / AGV / BENCH / TURBIDITY',
                                       `ownership_type` VARCHAR(16)  NOT NULL COMMENT '归属类型:EXCLUSIVE 独占 / SHARED 共享',
                                       `zone_code`      VARCHAR(32)           DEFAULT NULL COMMENT '独占时必填(归属区域),共享时为空',
                                       `max_concurrent` INT          NOT NULL DEFAULT 1 COMMENT '最大并发持有数,常规为1(同一时刻只能一个流程用),特殊场景可设大',
                                       `enabled`        BIT(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用,0=禁用不参与调度',
                                       `remark`         VARCHAR(256)          DEFAULT NULL COMMENT '备注,如"位于A区角落,物理遮挡"',

                                       `creator`        VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
                                       `create_time`    DATETIME(3)  NOT NULL COMMENT '创建时间',
                                       `updater`        VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
                                       `update_time`    DATETIME(3)  NOT NULL COMMENT '更新时间',
                                       `deleted`        BIT(1)       NOT NULL DEFAULT b'0' COMMENT '删除标记 0=正常 1=已删',

                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_resource_id` (`resource_id`),
                                       KEY `idx_type_zone` (`resource_type`, `zone_code`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源配置表,运行时锁状态见Redis';


-- ================================================================
-- lab_zone_quota 区域配额表
-- 控制各区域对共享资源池的最大借用数
-- 独占资源不受此表限制(独占本来就不能跨区)
-- ================================================================
CREATE TABLE `lab_zone_quota` (
                                  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `zone_code`     VARCHAR(32)  NOT NULL COMMENT '区域编码,如 ZONE-A / ZONE-B',
                                  `resource_type` VARCHAR(32)  NOT NULL COMMENT '共享资源类型,如 AGV',
                                  `max_borrow`    INT          NOT NULL COMMENT '本区最多同时借用数',
                                  `remark`        VARCHAR(256)          DEFAULT NULL COMMENT '配额说明',

                                  `creator`       VARCHAR(64)           DEFAULT NULL,
                                  `create_time`   DATETIME(3)  NOT NULL,
                                  `updater`       VARCHAR(64)           DEFAULT NULL,
                                  `update_time`   DATETIME(3)  NOT NULL,
                                  `deleted`       BIT(1)       NOT NULL DEFAULT b'0',

                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_zone_type` (`zone_code`, `resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域对共享资源的配额';