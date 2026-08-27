-- AGV 路由配置表
-- 每条记录描述一条完整的 AGV 搬运路线（step1 装载 → step2 移动 → step3 卸载）。
-- 补料/下料服务通过 route_code 动态读取，工作流编码不再硬编码于业务代码中。
--
-- 工作流编码来源：深度原理AGV工作流词典.xlsx
--   工作站        step1（装载到AGV）  step3（从AGV卸载）  孔位编号
--   后处理站       HCL_AGV            AGV_HCL             1~16
--   手套箱合成站   STX_AGV            AGV_STX             1~8
--   分析站         FX_AGV             AGV_FX              1~3
--   仓储站         CC_AGV             AGV_CC              1~2（中转位）
--   AGV 载架       -                  -                   1~8（7/8暂不用）
--
-- step2 工作流编码固定为 AgvMove，step2_station_name 须与 AGV 地图站点名称完全一致。
-- ⚠️ 执行前请确认 step2_station_name 与实际 AGV 地图配置对齐。

CREATE TABLE IF NOT EXISTS lab_agv_route_config
(
    id                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    route_code           VARCHAR(64)   NOT NULL COMMENT '路由唯一编码（业务查询键），格式建议 {起始站}_TO_{目标站}',
    route_name           VARCHAR(128)  NOT NULL COMMENT '路由名称，可读描述',

    -- 起始/目标站点（对应 AGV 地图区域标识）
    source_station_code  VARCHAR(64)   NOT NULL COMMENT '起始站点编码，如 HCL / WAREHOUSE / STX / FX',
    dest_station_code    VARCHAR(64)   NOT NULL COMMENT '目标站点编码，如 WAREHOUSE / HCL / STX / FX',

    -- AGV 基础参数（透传给 AGV 设备，由 AGV 厂商定义）
    task_type            VARCHAR(16)            COMMENT 'AGV任务类型，通常为 1',
    plate_type           VARCHAR(16)            COMMENT 'AGV托板类型，通常为 1',

    -- step1：物料装载到 AGV（站位/仓储中转 → AGV 载架）
    step1_task_name      VARCHAR(64)   NOT NULL COMMENT 'step1工作流编码：HCL_AGV/STX_AGV/FX_AGV/CC_AGV',

    -- step2：AGV 整体移动到目标站点
    step2_task_name      VARCHAR(64)   NOT NULL COMMENT 'step2工作流编码，固定为 AgvMove',
    step2_station_name   VARCHAR(64)   NOT NULL COMMENT 'step2目标站点名，须与AGV地图站点名称完全一致',

    -- step3：AGV 将物料卸载到目标位（AGV 载架 → 站位/仓储中转）
    step3_task_name      VARCHAR(64)   NOT NULL COMMENT 'step3工作流编码：AGV_HCL/AGV_STX/AGV_FX/AGV_CC',

    -- 通用配置
    enabled              TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    remark               VARCHAR(256)           COMMENT '备注',

    -- 审计字段（由 BaseDO 框架自动填充）
    creator              VARCHAR(64)            DEFAULT '' COMMENT '创建者',
    create_time          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updater              VARCHAR(64)            DEFAULT '' COMMENT '最后更新者',
    update_time          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    deleted              TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_route_code (route_code, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'AGV路由配置表';


-- ============================================================================
-- 初始化数据（来源：深度原理AGV工作流词典.xlsx）
-- ⚠️ step2_station_name 请对照实际 AGV 地图站点名称确认后修改
-- ============================================================================

-- ── 后处理站（HCL，16个孔位）────────────────────────────────────────────────

-- 补料：仓储 → 后处理工站
-- CC_AGV（仓储中转→AGV）→ AgvMove 到后处理 → AGV_HCL（AGV→站位，孔位1~16）
INSERT INTO lab_agv_route_config (route_code, route_name,
                                   source_station_code, dest_station_code,
                                   task_type, plate_type,
                                   step1_task_name, step2_task_name, step2_station_name, step3_task_name,
                                   enabled, remark)
VALUES ('WAREHOUSE_TO_HCL', '仓储 → 后处理工站（补料）',
        'WAREHOUSE', 'HCL', '1', '1',
        'CC_AGV', 'AgvMove', 'HCL_STATION', 'AGV_HCL',
        1, '仓储出库→AGV装载→移动到后处理站→卸料到站位（孔位编号1~16）');

-- 下料：后处理工站 → 仓储
-- HCL_AGV（站位→AGV）→ AgvMove 到仓储 → AGV_CC（AGV→仓储中转，孔位1~2）
INSERT INTO lab_agv_route_config (route_code, route_name,
                                   source_station_code, dest_station_code,
                                   task_type, plate_type,
                                   step1_task_name, step2_task_name, step2_station_name, step3_task_name,
                                   enabled, remark)
VALUES ('HCL_TO_WAREHOUSE', '后处理工站 → 仓储（下料）',
        'HCL', 'WAREHOUSE', '1', '1',
        'HCL_AGV', 'AgvMove', 'WAREHOUSE', 'AGV_CC',
        1, '后处理站位→AGV装载→移动到仓储→卸料到仓储中转位（孔位1~2），仓储机械臂逐件入库');

-- ── 手套箱合成站（STX，8个孔位）─────────────────────────────────────────────

-- 补料：仓储 → 手套箱合成站
-- CC_AGV → AgvMove 到手套箱 → AGV_STX（孔位1~8）
INSERT INTO lab_agv_route_config (route_code, route_name,
                                   source_station_code, dest_station_code,
                                   task_type, plate_type,
                                   step1_task_name, step2_task_name, step2_station_name, step3_task_name,
                                   enabled, remark)
VALUES ('WAREHOUSE_TO_STX', '仓储 → 手套箱合成站（补料）',
        'WAREHOUSE', 'STX', '1', '1',
        'CC_AGV', 'AgvMove', 'STX_STATION', 'AGV_STX',
        1, '仓储出库→AGV装载→移动到手套箱合成站→卸料到站位（孔位编号1~8）');

-- 下料：手套箱合成站 → 仓储
-- STX_AGV → AgvMove 到仓储 → AGV_CC（孔位1~2）
INSERT INTO lab_agv_route_config (route_code, route_name,
                                   source_station_code, dest_station_code,
                                   task_type, plate_type,
                                   step1_task_name, step2_task_name, step2_station_name, step3_task_name,
                                   enabled, remark)
VALUES ('STX_TO_WAREHOUSE', '手套箱合成站 → 仓储（下料）',
        'STX', 'WAREHOUSE', '1', '1',
        'STX_AGV', 'AgvMove', 'WAREHOUSE', 'AGV_CC',
        1, '手套箱站位→AGV装载→移动到仓储→卸料到仓储中转位（孔位1~2），仓储机械臂逐件入库');

-- ── 分析站（FX，3个孔位）────────────────────────────────────────────────────

-- 补料：仓储 → 分析站
-- CC_AGV → AgvMove 到分析站 → AGV_FX（孔位1~3）
INSERT INTO lab_agv_route_config (route_code, route_name,
                                   source_station_code, dest_station_code,
                                   task_type, plate_type,
                                   step1_task_name, step2_task_name, step2_station_name, step3_task_name,
                                   enabled, remark)
VALUES ('WAREHOUSE_TO_FX', '仓储 → 分析站（补料）',
        'WAREHOUSE', 'FX', '1', '1',
        'CC_AGV', 'AgvMove', 'FX_STATION', 'AGV_FX',
        1, '仓储出库→AGV装载→移动到分析站→卸料到站位（孔位编号1~3）');

-- 下料：分析站 → 仓储
-- FX_AGV → AgvMove 到仓储 → AGV_CC（孔位1~2）
INSERT INTO lab_agv_route_config (route_code, route_name,
                                   source_station_code, dest_station_code,
                                   task_type, plate_type,
                                   step1_task_name, step2_task_name, step2_station_name, step3_task_name,
                                   enabled, remark)
VALUES ('FX_TO_WAREHOUSE', '分析站 → 仓储（下料）',
        'FX', 'WAREHOUSE', '1', '1',
        'FX_AGV', 'AgvMove', 'WAREHOUSE', 'AGV_CC',
        1, '分析站站位→AGV装载→移动到仓储→卸料到仓储中转位（孔位1~2），仓储机械臂逐件入库');
