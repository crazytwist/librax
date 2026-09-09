-- AI 聊天对话：新增意图字段（设备/资源/流程/其他）
-- 执行前请确认表 ai_chat_conversation 已存在

ALTER TABLE `ai_chat_conversation`
    ADD COLUMN `intent` varchar(32) NULL DEFAULT NULL COMMENT '对话意图：DEVICE/RESOURCE/FLOW/OTHER' AFTER `role_id`;
