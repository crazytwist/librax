-- ================================================================
-- 示例:水质检测场景的资源配置
-- 独占:PH 仪 / 浊度仪 / 采样器(归属 ZONE-A)
-- 共享:AGV(公共池,2辆)
-- ================================================================

-- 独占资源(ZONE-A 的仪器工位)
INSERT INTO lab_resource_config
(resource_id, resource_type, ownership_type, zone_code, max_concurrent, enabled,
 remark, create_time, update_time) VALUES
                                       ('SAMPLER-01',   'SAMPLER',   'EXCLUSIVE', 'ZONE-A', 1, b'1', 'A区采样器',    NOW(3), NOW(3)),
                                       ('PH-METER-01',  'PH_METER',  'EXCLUSIVE', 'ZONE-A', 1, b'1', 'A区PH测量仪',  NOW(3), NOW(3)),
                                       ('TURBIDITY-01', 'TURBIDITY', 'EXCLUSIVE', 'ZONE-A', 1, b'1', 'A区浊度仪',    NOW(3), NOW(3));

-- 共享资源(AGV 不属于任何区)
INSERT INTO lab_resource_config
(resource_id, resource_type, ownership_type, zone_code, max_concurrent, enabled,
 remark, create_time, update_time) VALUES
                                       ('AGV-01', 'AGV', 'SHARED', NULL, 1, b'1', '公共AGV 01号', NOW(3), NOW(3)),
                                       ('AGV-02', 'AGV', 'SHARED', NULL, 1, b'1', '公共AGV 02号', NOW(3), NOW(3));

-- 区域配额:ZONE-A 最多同时借用 2 辆 AGV(共享池总共2辆,也就是能独占)
INSERT INTO lab_zone_quota
(zone_code, resource_type, max_borrow, remark, create_time, update_time) VALUES
    ('ZONE-A', 'AGV', 2, 'A区最多借2辆AGV',  NOW(3), NOW(3));

-- 如果未来加 ZONE-B,可以配 'ZONE-B', 'AGV', 1 防止把 AGV 全占