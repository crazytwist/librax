-- ================================================================
-- 流程步骤设计器菜单 SQL
-- 在「流程管理」父菜单下添加「流程步骤配置」隐藏页
-- 执行前：将 @FLOW_PARENT_ID 替换为实际的「流程管理」目录菜单ID
-- ================================================================

-- 1. 查询当前「流程管理」目录的 parent_id，确认后填入下面的 parent_id
--    SELECT id, name, path FROM system_menu WHERE name LIKE '%流程%' AND type IN (1,2);

-- 2. 添加「流程步骤配置器」菜单页（hidden，从流程定义列表跳转进入）
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '流程步骤配置器',
    '',
    2,       -- type=2 菜单
    99,      -- sort，排在最后
    (SELECT id FROM system_menu WHERE name = '流程管理' AND type = 1 LIMIT 1),
    'pipeline-designer',             -- 路由 path（相对父级）
    'ep:connection',                 -- 图标
    'flow/pipelinedesigner/index',   -- Vue 组件路径
    'PipelineDesigner',              -- component_name
    0,       -- status=0 正常
    b'0',    -- visible=0 隐藏（不在侧边菜单显示）
    b'0',    -- keep_alive=0（每次进入重新加载数据）
    b'0',
    '1', NOW(), '1', NOW(), b'0'
);

-- 3. 获取刚插入的菜单ID（用于后续权限按钮）
-- SET @DESIGNER_MENU_ID = LAST_INSERT_ID();

-- 4. 添加操作权限按钮（关联到「流程步骤配置器」菜单下）
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
-- 查询（用于「流程定义」列表页的「配置步骤」按钮权限）
(
    '步骤配置查询', 'flow:pipeline-step:query', 3, 1,
    LAST_INSERT_ID(),
    '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
),
(
    '步骤配置新增', 'flow:pipeline-step:create', 3, 2,
    (SELECT id FROM system_menu WHERE component = 'flow/pipelinedesigner/index' LIMIT 1),
    '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
),
(
    '步骤配置编辑', 'flow:pipeline-step:update', 3, 3,
    (SELECT id FROM system_menu WHERE component = 'flow/pipelinedesigner/index' LIMIT 1),
    '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
),
(
    '步骤配置删除', 'flow:pipeline-step:delete', 3, 4,
    (SELECT id FROM system_menu WHERE component = 'flow/pipelinedesigner/index' LIMIT 1),
    '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
);

-- ================================================================
-- 如果上面的子查询有问题，可以用下面的两步写法：
-- Step 1: 先插入主菜单，记录 ID
-- Step 2: 把 ID 填入下面的权限按钮 parent_id
-- ================================================================
/*
-- 备用写法（手动指定 parent_id）：
-- 把 XXXXX 替换为「流程管理」目录菜单的 id（通过上面 SELECT 查询获得）
-- 把 YYYYY 替换为新插入的「流程步骤配置器」菜单的 id（LAST_INSERT_ID() 或自增值）

INSERT INTO `system_menu` (...) VALUES (
    '流程步骤配置器', '', 2, 99, XXXXX,
    'pipeline-designer', 'ep:connection',
    'flow/pipelinedesigner/index', 'PipelineDesigner',
    0, b'0', b'0', b'0', '1', NOW(), '1', NOW(), b'0'
);

INSERT INTO `system_menu` (...) VALUES
('步骤配置查询',  'flow:pipeline-step:query',  3, 1, YYYYY, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
('步骤配置新增',  'flow:pipeline-step:create', 3, 2, YYYYY, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
('步骤配置编辑',  'flow:pipeline-step:update', 3, 3, YYYYY, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
('步骤配置删除',  'flow:pipeline-step:delete', 3, 4, YYYYY, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');
*/
