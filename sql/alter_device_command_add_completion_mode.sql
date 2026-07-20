-- 为 lab_device_command 表新增 completion_mode 字段
-- completionMode 值：
--   WEBHOOK（默认/NULL）— 设备异步回调后推进流程
--   SYNC              — HTTP 响应返回即视为完成，直接推进下一节点
--   POLL              — 定期轮询查询结果（配合 poll_path / poll_done_expr 使用）
ALTER TABLE lab_device_command
    ADD COLUMN completion_mode VARCHAR(20) NULL COMMENT '指令完成模式：WEBHOOK/SYNC/POLL，NULL 等同 WEBHOOK'
    AFTER codec_id;
